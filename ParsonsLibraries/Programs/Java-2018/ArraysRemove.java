/*
[#author#]
EvoParsons Team

[#language#]
java

[#title#]
ArraysRemove

[#description#]
This program starts by generating an array of random integer values.
It then removes the value at a given random index inside that array, using the remove method.
This method creates a new array with one less element compared to the array passed as parameter.
The element at the index also specified as parameter does not appear in the returned array.
All other values are from the array passed as parameter.
The method asString returns a string representing the contents of an array passed as parameter.
The method getRandomInt uses (Math.random()*max) to generate the elements of an array randomly between [0..max).
The method getRandomArray uses the above to generate a random array which size is specified as parameter.
We assume that getRandomArray will return an array where every element is unique.
Implement these method in the following order; main, remove, asString, getRandomInt, getRandomArray.

[#source#]
*/
import java.util.Scanner; \\npublic class ArraysRemove {\\n  public static void main(String[] args){
      int arraySize = 10, deletionIndex = getRandomInt(arraySize);  \\nint[] myArray = getRandomArray(arraySize);  \\nSystem.out.println("Removing element at index " + deletionIndex + " from array " + asString(myArray));\\nSystem.out.println("Result is new array " + asString(remove(myArray, deletionIndex)));\\n} //end of main method
   public static int[] remove(int[] array, int index){
      int[] result1 = new int[array.length-1];
      for(int n = 0; n < result1.length ; n++) { \\n if(n < index) result1[n] = array[n]; \\n else result1[n] = array[n+1]; \\n }
      return result1; \\n } //end of remove method
   public static String asString(int[] a){  \\nString result = "";  \\nfor(int i=0; i < a.length; i++){  \\nresult = result + a[i]+ " "; \\n } \\nreturn result;  \\n} // end of asString method
   public static int getRandomInt(int max){\\n   return (int)(Math.random()*max);\\n} // end of getRandomInt method
   public static int[] getRandomArray(int size){\\nint[] data = new int[size];\\nfor(int i = 0 ; i < data.length ; ){ \\n   data[i++] = getRandomInt(10);\\n } \\n return data;\\n} // end of getRandomArray method \\n} // end of class
