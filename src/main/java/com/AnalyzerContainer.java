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
            case "english":
                return new EnglishAnalyzer();
            case "latvian":
                return new LatvianAnalyzer();
            case "lithuanian":
                return new LithuanianAnalyzer();
            case "estonian":
                return new EstonianAnalyzer();
            case "russian":
                return new RussianAnalyzer();
            case "german":
                return new GermanAnalyzer();
            case "polish":
                return new PolishAnalyzer();
            default:
                throw new RuntimeException("Invalid analyzer option: " + lang);
        }

    }

}
