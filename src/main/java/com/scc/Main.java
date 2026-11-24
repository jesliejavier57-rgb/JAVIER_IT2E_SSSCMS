package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        Scanner scanner = new Scanner(System.in);
        Authentication auth = new Authentication(scanner);
        auth.start();
        scanner.close();
    }
}