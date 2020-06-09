package com;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.et.EstonianAnalyzer;
import org.apache.lucene.analysis.lt.LithuanianAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;


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
            case "fr":
                return new FrenchAnalyzer();
            case "it":
                return new ItalianAnalyzer();
            case "es":
                return new SpanishAnalyzer();
            case "pt":
                return new PortugueseAnalyzer();
            case "el":
                return new GreekAnalyzer();
            case "ro":
                return new RomanianAnalyzer();
            case "bg":
                return new BulgarianAnalyzer();
            case "hu":
                return new HungarianAnalyzer();
            case "cz":
                return new CzechAnalyzer();
            case "ga":
                return new IrishAnalyzer();
            case "fi":
                return new FinnishAnalyzer();
            case "no":
                return new NorwegianAnalyzer();
            case "sv":
                return new SwedishAnalyzer();
            case "nl":
                return new DutchAnalyzer();
            default:
                throw new RuntimeException("Invalid analyzer option: " + lang);
        }

    }

}
