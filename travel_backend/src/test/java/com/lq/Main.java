//package com.lq;
//
//import java.util.*;
//
//// 注意类名必须为 Main, 不要有任何 package xxx 信息
//public class Main {
//    public static void main(String[] args) {
//
//        System.out.println();
//
//    }
//
//    public int longestConsecutive(int[] nums) {
//        Set<Integer> numSet = new HashSet<>();
//        for (int num : nums) {
//            numSet.add(num);
//        }
//        int longestStreak = 0;
//        for (int num : numSet) {
//            if (!numSet.contains(num - 1)) {
//                int currentNum = num;
//                int currentStreak = 1;
//                while (numSet.contains(currentNum + 1)) {
//                    currentNum += 1;
//                    currentStreak += 1;
//                }
//                longestStreak = Math.max(longestStreak, currentStreak);
//            }
//        }
//
//        return longestStreak;
//    }
//}
//
