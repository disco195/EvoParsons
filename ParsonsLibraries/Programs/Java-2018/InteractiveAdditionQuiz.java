/*
[#author#]
EvoParsons Team

[#language#]
java

[#title#]
InteractiveAdditionQuiz

[#description#]
This program repeatedly prompts the user to enter the result of adding two random integers until the user provided 5 correct responses.
The method getIntFromUser creates a new Scanner and, reads an int value from the user, then returns it.
The method getRandomInt uses (Math.random()*10+1) to generate a random values in [1..10] that it then returns.
Implement these method in the following order; main, getRandomInt, getIntFromUser.

[#source#]
*/
import java.util.Scanner; \\npublic class InteractiveAdditionQuiz {
   public static void main(String[] args){   
      int wins = 0, required = 5;
      while(wins < required){
         int n1 = getRandomInt(), n2 = getRandomInt();
         System.out.println(n1 + " + " + n2 + " = ");
         int answer = getIntFromUser();
         if(answer == (n1 + n2))
            System.out.println("Correct Answer (" + (++wins) + " so far)");
         else System.out.println("Incorrect Answer");
      } // end of while loop
      System.out.println("Congratulations, you provided " + required + " correct answers.");\\n} //end of main method
   public static int getRandomInt(){\\n return (int)(Math.random()*10+1);\\n} // end of getRandomInt method
   public static int getIntFromUser(){
      Scanner s = new Scanner(System.in);
      return s.nextInt();
   } // end of getIntFromUser method\\n} // end of class