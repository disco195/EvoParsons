package evoparsons.broker;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import evoparsons.rmishared.ParsonsEvaluation;
import evoparsons.rmishared.ParsonsPuzzle;
import evoparsons.rmishared.Stats;

public class EvaluationDataStore
{		
	private static final String DEFAULT_STUDENTS_FILE = "students.bro";
	private static final String DEFAULT_STUDENT_STAT_FILE = "studentStat.bro";
	private static final String DEFAULT_GENOTYPE_FILE = "genotype.bro";
	private String outputFolder;

	private Map<String, Integer> students; //TODO: cen we remove it
	private Map<Integer, Stats> studentStats;
	private Map<Integer, PuzzleEvaluation> genotypes;
	private Map<Integer, PuzzleEvaluation> currentGenerationGenotypes;
	private String studentsFile;
	private String studentStatsFile;
	private String genotypesFile;
	private int evalTries; 	
	private Log log;
	private Config config;

	public EvaluationDataStore(Config config)
	{
		this.log = config.getLog();
		this.config = config;
		this.outputFolder = config.getOutputFolder();
		try {
			String evalTries = config.get("evoparsons.evalTries", "");
			this.evalTries = Integer.parseInt(evalTries);
		} catch (NumberFormatException e) {
			log.log("[EvaluationDataStore] Using default value for evalTries=2");
			this.evalTries = 2;
		}		
		studentsFile = config.get("evoparsons.studentsFile", DEFAULT_STUDENTS_FILE);
		students = Utils.<HashMap<String, Integer>>loadFromFile(log, Paths.get(outputFolder, studentsFile).toString(), HashMap<String, Integer>::new);
		if (students.size() == 0)
			log.log("[EvaluationDataStore] students hash is empty");
		else 
		{
			log.log("[EvaluationDataStore] %d students were restored from %s", students.size(), studentsFile);
			students.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getValue()))
				.forEach(entry -> log.log("\t%8.8s%6d", entry.getKey(), entry.getValue()));
		}
		studentStatsFile = config.get("evoparsons.studentsStatsFile", DEFAULT_STUDENT_STAT_FILE);
		studentStats = Utils.<HashMap<Integer, Stats>>loadFromFile(log, Paths.get(outputFolder, studentStatsFile).toString(), HashMap<Integer, Stats>::new);
		genotypesFile = config.get("evoparsons.genotypesFile", DEFAULT_GENOTYPE_FILE);
		genotypes = 
			Utils.<Map<Integer, PuzzleEvaluation>>loadFromFile(log, Paths.get(outputFolder, genotypesFile).toString(), 
				HashMap<Integer, PuzzleEvaluation>::new);
		//FIX in timestamp field
		// genotypes
		// 	.entrySet()
		// 	.forEach(entry -> 
		// 		entry.getValue().evaluations
		// 			.entrySet()
		// 			.forEach(entry2 -> {
		// 				if (entry2.getValue().timestamp == 0) entry2.getValue().timestamp = Long.MIN_VALUE;
		// 			}));
		if (genotypes.size() == 0)
			log.log("[EvaluationDataStore] genotypes hash is empty");
		else
		{
			log.log("[EvaluationDataStore] %d genotypes were restored from %s", genotypes.size(), genotypesFile);
		}
		currentGenerationGenotypes = 
			genotypes.entrySet().stream()
				.collect(Collectors.groupingBy(entry -> entry.getValue().generation))
				.entrySet().stream()
				.max(Comparator.comparing(entry -> entry.getKey()))
				.map(entry -> entry.getValue())
				.orElse(new ArrayList<Entry<Integer, PuzzleEvaluation>>())
				.stream()
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
		if (currentGenerationGenotypes.size() > 0)
			log.log("[EvaluationDataStore] %d genotypes in current generation %d", currentGenerationGenotypes.size(), 
				currentGenerationGenotypes.entrySet().stream().findFirst().map(g -> g.getValue().generation).get());
	}

	public boolean hasStudents() { return students.size() > 0; }

	public void printInteractions(Library lib) {
		log.log("--------------------------------------------");
		String padding = new String(new char[13]).replace('\0', ' ');
		log.print(padding);
		genotypes.entrySet().stream()
			.sorted(Comparator.comparing(entry -> entry.getKey()))
			.forEach(genotype -> {		
				ParsonsPuzzle puzzle = genotype.getValue().genotype.getPuzzle(lib);	
				int programSize = puzzle.program.size();
				int appliedDistractorsCount = puzzle.distracters.size();	
				log.print("%10.10s",
					String.format(
						currentGenerationGenotypes.containsKey(genotype.getKey()) ? "%d,%d,%d*" : "%d,%d,%d", 
						genotype.getValue().genotype.genome[0],
						programSize,
						appliedDistractorsCount));
			});		
		log.log("");
		students.entrySet().stream()
			.sorted(Comparator.comparing(student -> student.getValue()))
			.forEach(student -> 
				{
					log.print("%13.13s", String.format("%8.8s[%d]", student.getKey(), student.getValue()));
					genotypes.entrySet().stream()
						.sorted(Comparator.comparing(entry -> entry.getKey()))
						.forEach(genotype -> {	
							if (!genotype.getValue().evaluations.containsKey(student.getValue())) log.print("%10.10s", "");
							else 
							{
								ParsonsEvaluation eval = genotype.getValue().evaluations.get(student.getValue());
								if (eval.gaveUp)
									log.print("%10.10s", "gaveUp");
								else 
									log.print("%10.10s", String.format("%d,%d,%.2f",
										eval.moves, eval.timeInMs / 1000, eval.fitness));
							}
						});
					log.log("");
				});		
		log.log("--------------------------------------------");	
	}

	public void addGenotypes(List<ParsonsGenotype> g, int generation)
	{
		currentGenerationGenotypes = 
			g.stream()
				.collect(Collectors.toMap(genotype -> genotype.getIndex(),
					genotype ->
					{
						PuzzleEvaluation evals = genotypes.get(genotype.getIndex());
						if (evals == null)
							return new PuzzleEvaluation(genotype, generation);
						else
							return new PuzzleEvaluation(genotype, evals.evaluations, generation);
					}));
		genotypes.putAll(currentGenerationGenotypes);
	}	   
		
	public ParsonsPuzzle getPuzzle(int studentId, SelectionPolicy selectionPolicy, Library lib)
	{
		Optional<PuzzleEvaluation> genotypeOpt = 
			selectionPolicy.select(studentId, currentGenerationGenotypes, log);
		if (!genotypeOpt.isPresent()) {
			log.err("[EvaluationDataStore.getPuzzle] Selection policy did not return puzzle. Check configuration");
			System.exit(1);		
		} else {
			log.log("[EvaluationDataStore.getPuzzle] student %d got %s", studentId, genotypeOpt.get().genotype.toString());
		}
		return genotypeOpt.get().genotype.getPuzzle(lib);
	}

	public Stats getStudentStats(int studentId) {
		Stats stats = studentStats.computeIfAbsent(studentId, k -> new Stats(0, 0));
		return stats;
	}

	public void addEvaluation(ParsonsEvaluation eval, SelectionPolicy selectionPolicy)
	{			
		PuzzleEvaluation puzzleEval = genotypes.get(eval.puzzleIndex);				
		if (puzzleEval != null)
		{
			//selectionPolicy.setSeen(eval.studentId, puzzleEval);
			ParsonsEvaluation existingEval = puzzleEval.evaluations.get(eval.studentId);
			Stats stats = studentStats.get(eval.studentId);
			if (stats == null) {
				log.log("[EvaluationDataStore.addEvaluation] Cannot find stats for student %d. Creating new", eval.studentId);
				stats = new Stats(0, 0);
				studentStats.put(eval.studentId, stats);
			}
			stats.duration += eval.timeInMs;			
			if ((existingEval == null) || 
				(existingEval.gaveUp && !eval.gaveUp))
			{	
				if (existingEval == null) stats.puzzlesSeen++;
				if (!eval.gaveUp)
				{			
					stats.puzzlesSolved++;		
					int studentId = eval.studentId;
					int programIndex = puzzleEval.genotype.genome[0];
					boolean studentHasAlreadySeenPuzzle = 
						genotypes.entrySet().stream()
							.filter(entry -> 
								(entry.getValue().genotype.genome[0] == programIndex) 
								&& entry.getValue().evaluations.containsKey(studentId)
								&& !entry.getValue().evaluations.get(studentId).gaveUp)
							.findAny().isPresent();
					eval =
						studentHasAlreadySeenPuzzle ?
							new ParsonsEvaluation(studentId, eval.puzzleIndex, 
								eval.moves, eval.timeInMs,
								DoubleStream.concat(DoubleStream.of(eval.fitness),
									puzzleEval.evaluations.entrySet().stream()
										.filter(entry -> !entry.getValue().gaveUp)
										.mapToDouble(entry -> entry.getValue().fitness))
										.average()
										.getAsDouble(),							
								eval.gaveUp, eval.timestamp) : 
							eval;
				}		
				puzzleEval
					.evaluations
					.put(eval.studentId, eval);				
			} else if (existingEval != null) {
				existingEval.setTimeStamp(eval.timestamp);
			}			
		} else
			log.err("[EvaluationDataStore.addEvaluation] Cannot find genotype for eval %s", eval.toString());		
	}

	public int addStudent(String studentName)
	{
		Integer existingId = students.putIfAbsent(studentName, students.size());		
		if (existingId == null)
		{			
			studentStats.put((students.size() - 1), new Stats(0, 0));
			log.log("[EvaluationDataStore.addStudent] login %s, %d", studentName, (students.size() - 1));
			return students.size() - 1;
		}
		else {
			log.log("[EvaluationDataStore.addStudent] continue session %s, %d", studentName, existingId);	
			return existingId;
		}	
	}

	/**
	 * Each time a ParsonsEvaluation data is sent to the broker, it checks
	 * if the problet and its pair is evaluated minimum number of evaluation
	 * by same set of students
	 * @param data
	 * @return
	 */
	
	public ParsonsFitness getFitness(ParsonsEvaluation eval, Library lib)
	{	
		if (eval.gaveUp) return null;
		PuzzleEvaluation genotypeEvals = 
			currentGenerationGenotypes.get(eval.puzzleIndex);
		if (genotypeEvals == null)  // this evaluation is from previous generation
		{
			log.log("[EvaluationDataStore.getFitness] Eval %d, %d from prev. generation, ignored", eval.studentId, eval.puzzleIndex);
			return null;
		}
				
		PuzzleEvaluation pairedGenotypeEvals = 
			currentGenerationGenotypes.get(genotypeEvals.genotype.getPairedIndex());			
		if (pairedGenotypeEvals == null) 
		{
			return null;
		}
			    
		class CommonEvaluations {
			public final List<Double> firstGenotypeEvals;
			public final List<Double> secondGenotypeEvals;
			public final List<Integer> students;
			public CommonEvaluations() {
				firstGenotypeEvals = new ArrayList<>();
				secondGenotypeEvals = new ArrayList<>();
				students = new ArrayList<>();
			}
		}

		DoubleFunction<Double> round = 	value -> Math.round(value * 1000) / 1000.0;			
		
		CommonEvaluations commonEvals = 
			genotypeEvals.evaluations.entrySet().stream()
				.filter(entry -> 
					!entry.getValue().gaveUp
					&& pairedGenotypeEvals.evaluations.containsKey(entry.getKey())
					&& !pairedGenotypeEvals.evaluations.get(entry.getKey()).gaveUp)
				.collect(
					() -> new CommonEvaluations(),
					(acc, entry) -> {
						acc.firstGenotypeEvals.add(round.apply(entry.getValue().fitness));
						acc.secondGenotypeEvals.add(round.apply(pairedGenotypeEvals.evaluations.get(entry.getKey()).fitness));
						acc.students.add(entry.getKey());
					},
					(acc1, acc2) -> {}
				);

		if (commonEvals.students.size() < evalTries) return null;

		log.log("[EvaluationDataStore.getFitness] (%d, %d) is ready%n%10.10s%s%n%10.10s%s%n%10.10s%s", 
			genotypeEvals.genotype.getIndex(), pairedGenotypeEvals.genotype.getIndex(),
			"students:",
			commonEvals.students.stream().map(i -> String.format("%6d", i)).collect(Collectors.joining(" ")),
			"first:",
			commonEvals.firstGenotypeEvals.stream().map(d -> String.format("%6.2f", d)).collect(Collectors.joining(" ")),
			"second:",
			commonEvals.secondGenotypeEvals.stream().map(d -> String.format("%6.2f", d)).collect(Collectors.joining(" "))
			);		
		return ParsonsFitness.Create(genotypeEvals.genotype, commonEvals.firstGenotypeEvals.stream().mapToDouble(x -> x).toArray(), 
			pairedGenotypeEvals.genotype, commonEvals.secondGenotypeEvals.stream().mapToDouble(x -> x).toArray());				
	}

	public void saveStudents()
	{
		if (!students.isEmpty())
			Utils.saveToFile(log, students,  Paths.get(outputFolder, studentsFile).toString());
		else
			log.log("[EvaluationDataStore.saveStudents] Students database is empty!");
	}

	public void saveStudentStats() {
		if (!studentStats.isEmpty())
		{				
			Utils.saveToFile(log, studentStats, Paths.get(outputFolder, studentStatsFile).toString());
		}
		else
			log.log("[EvaluationDataStore.saveStudentStats] studentStats database is empty!");		
	}

	public void saveGenotypes()
	{
		if (!genotypes.isEmpty())
		{				
			Utils.saveToFile(log, genotypes, Paths.get(outputFolder, genotypesFile).toString());
		}
		else
			log.log("[EvaluationDataStore.saveGenotypes] Genotypes database is empty!");
	}	

}
