package com;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.et.EstonianAnalyzer;
import org.apache.lucene.analysis.lt.LithuanianAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;

public class AnalyzerContainer {
    public static Analyzer getAnalyzer(String lang) {
        switch (lang){
            case "en":
                return new EnglishAnalyzer();
            case "lv":
                return new LatvianAnalyzer();
            case "lt":
                return new LithuanianAnalyzer();
            case "et":
                return new EstonianAnalyzer();
            case "ru":
                return new RussianAnalyzer();
            case "de":
                return new GermanAnalyzer();
            case "pl":
                return new PolishAnalyzer();
            default:
                throw new RuntimeException("Invalid analyzer option: " + lang);
        }

    }

}