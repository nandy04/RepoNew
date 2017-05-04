package hackerRank.sample1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class HackerShirts {
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
        int q = in.nextInt();
        for(int a0 = 0; a0 < q; a0++){
            int n = in.nextInt();
            int[] sizes = new int[n];
            TreeMap<Integer, String> sizesMap = new TreeMap<Integer, String>();

            for(int sizes_i=0; sizes_i < n; sizes_i++){
                sizes[sizes_i] = in.nextInt();
                sizesMap.put(sizes[sizes_i],"");
            }
            
//            HashMap<Integer,List<Integer>> map=new HashMap<Integer, List<Integer>>();
//            List<Integer> list = new ArrayList<Integer>();
            int count=0;            
            SortedMap<Integer, String> treemap = new TreeMap<Integer, String>();

            int v = in.nextInt();
            for(int a1 = 0; a1 < v; a1++){
                int smallest = in.nextInt();
                int largest = in.nextInt();
                treemap = new TreeMap<Integer, String>();
                treemap=sizesMap.subMap(smallest, largest+1);
                int size=treemap.size();
                if(size>0){
                	count+=size;
                    sizesMap.subMap(smallest, largest+1).clear();
                }
            }
            
            System.out.println(count);
        }
    }
}
/*
2
5
2 3 6 9 13
4
14 97 
4 8
14 16
10 13
2
3 2
2
3 4
4 5 
 * 
 */
