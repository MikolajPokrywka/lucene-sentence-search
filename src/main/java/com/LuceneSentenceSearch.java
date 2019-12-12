package com;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import java.util.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.codecs.lucene80.Lucene80Codec;



public class LuceneSentenceSearch {
    private static int MAX_SENTENCE_LENGTH_BPE = 120;
    private static int NUMBER_OF_CONTEXT_CANDIDATES = 1000;
    private Directory indexDirectory;
    private Analyzer analyzer;
    private static final String INSERT = "INSERT";
    private static final String GET = "GET";
    private static final String DELETE = "DELETE";
    private static int CONTEXT_COUNT = 50;
    private static float BLEU_SIMILARITY_THRESHOLD = 0.05f;

    private static final Logger LOGGER = Logger.getLogger("Lucene translation memory.");

    private LuceneSentenceSearch(Directory fsDirectory) {
        super();
        this.indexDirectory = fsDirectory;
        this.analyzer = new StopwordAnalyzerBase() {
            final List<String> stopWords = Arrays.asList(
                    "a", "an", "and", "are", "as", "at", "be", "but", "by",
                    "for", "if", "in", "into", "is", "it",
                    "no", "not", "of", "on", "or", "such",
                    "that", "the", "their", "then", "there", "these",
                    "they", "this", "to", "was", "will", "with"
            );
            final CharArraySet stopSet = new CharArraySet(stopWords, true);
            final CharArraySet ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);

            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                final Tokenizer source = new StandardTokenizer();
                TokenStream result = new EnglishPossessiveFilter(source);

                result = new StopFilter(result, ENGLISH_STOP_WORDS_SET);

                result = new PorterStemFilter(result);
                return new TokenStreamComponents(source, result);
            }

        };

    }

    private static Comparator<ScoreDoc> scoreDocComparator = new Comparator<ScoreDoc>() {
        @Override
        public int compare(ScoreDoc d1, ScoreDoc d2) {
            if (d1.score > d2.score)
                return -1;
            else if (d1.score > d2.score)
                return 1;
            else
                return 0;
        }
    };

    public static LuceneSentenceSearch createIndex() {
        String indexName = RandomStringUtils.randomAlphabetic(10);
        String indexDir = "index//" + indexName;

        LuceneSentenceSearch indexer = null;
        try {

            Path indexPath = Paths.get(indexDir);
            MMapDirectory fsDirectory = new MMapDirectory(indexPath);
            indexer = new LuceneSentenceSearch(fsDirectory);
        } catch (Exception ex) {
            System.out.println("Cannot create index..." + ex.getMessage());
            System.exit(-1);
        }
        return indexer;
    }

    public static void main(String[] args) {
        LuceneSentenceSearch indexer = createIndex();
        Scanner sc = new Scanner(System.in);
        String command;
        String line;
        String UID = "";
        Query query;
        int i=0;
        while (sc.hasNextLine()) {
            line = sc.nextLine();

            command = line.split("\t")[0];
            switch (command) {
                case GET:
                    UID = line.split("\t")[1];
                    String reference = line.split("\t")[2];

                    try {
                        reference = reference.replaceAll("@@ ", "");
                        List<Document> contextSentences = indexer.queryTM(reference, UID);
                        for (Document contextSentence : contextSentences) {
                            String srcSent = contextSentence.getField("srcBPE").stringValue();
                            String trgSent = contextSentence.getField("trgBPE").stringValue();
                            System.out.println(srcSent + " ||| " + trgSent);
                        }

                    } catch (ParseException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Exception while handling GET for UID: " + UID);
                        LOGGER.log(Level.SEVERE, "Exception while handling GET for sentence: " + reference);
                        LOGGER.log(Level.SEVERE, "Exception", e);
                        e.printStackTrace(System.out);
                    }
                    System.out.println("\n");
                    break;
                case INSERT:
                    UID = line.split("\t")[1];
                    String src = line.split("\t")[2];
                    String trg = line.split("\t")[3];
                    try {
                        indexer.addSentenceToIndex(src, trg, UID);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Exception while handling INSERT for UID: " + UID);
                        LOGGER.log(Level.SEVERE, "Exception while handling INSERT for SRC sentence: " + src);
                        LOGGER.log(Level.SEVERE, "Exception while handling INSERT for TRG sentence: " + trg);
                        e.printStackTrace(System.out);
                        LOGGER.log(Level.SEVERE, "Exception", e);
                    }
                    break;
                case DELETE:
                    UID = line.split("\t")[1];
                    try {
                        indexer.deleteSentenceFromIndexByUID(UID);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Exception while handling DELETE for UID: " + UID);
                        LOGGER.log(Level.SEVERE, "Exception", e);
                        e.printStackTrace(System.out);
                    }
                default:
                    break;
            }
        }
    }

    public void addSentenceToIndex(String srcSent, String trgSent, String UID) throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setCodec(new Lucene80Codec());
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
        Document sentence = new Document();
        String[] srcParts = srcSent.split(" ");
        String[] trgParts = trgSent.split(" ");

        if (srcParts.length <= MAX_SENTENCE_LENGTH_BPE && trgParts.length <= MAX_SENTENCE_LENGTH_BPE) {
            sentence.add(new TextField("src", srcSent.replaceAll("@@ ", ""), Field.Store.YES));
            sentence.add(new StringField("srcBPE", srcSent, Field.Store.YES));
            sentence.add(new StringField("trgBPE", trgSent, Field.Store.YES));
            sentence.add(new TextField("UID", UID, Field.Store.YES));
            sentence.add(new StoredField("timeStamp", Instant.now().getEpochSecond()));
            indexWriter.addDocument(sentence);
        }
        indexWriter.close();
    }

    public void deleteSentenceFromIndexByUID(String UID) throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setCodec(new Lucene80Codec());
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
        Query UIDQuery = new TermQuery(new Term("UID", UID));
        indexWriter.deleteDocuments(UIDQuery);
        indexWriter.commit();
        indexWriter.close();
    }


    private static ArrayList<Document> bleuRescorer(String reference, List<Document> candidates) {
        ArrayList<Document> contextSentences = new ArrayList<Document>();
        F1BleuCalculator calculator = new F1BleuCalculator(reference.split(" "));
        PriorityQueue<ScoreDoc> topScoringDocs = new PriorityQueue<ScoreDoc>(CONTEXT_COUNT, scoreDocComparator);
        for (int i = 0; i < candidates.size(); ++i) {
            Document hypothesisDoc = candidates.get(i);
            String hypothesis = hypothesisDoc.getField("src").stringValue();
            Float bleuScore = calculator.calc(hypothesis.split(" "));
            ScoreDoc scoredItem = new ScoreDoc(i, bleuScore);
            topScoringDocs.add(scoredItem);
        }
        int k = 0;
        while (!topScoringDocs.isEmpty() && k < CONTEXT_COUNT) {
            k += 1;
            ScoreDoc scoredItem = topScoringDocs.poll();
            if (scoredItem.score >= BLEU_SIMILARITY_THRESHOLD) {
                contextSentences.add(candidates.get(scoredItem.doc));
            }
        }
        return contextSentences;
    }

    public List<Document> queryTM(String reference, String UID) throws ParseException, IOException {
        Query query;
        query = createQuery(reference, UID);
        List<Document> candidateSentences = searchSentence(query);
        List<Document> contextSentences = bleuRescorer(reference, candidateSentences);
        return contextSentences;
    }

    public BooleanQuery createQuery(String queryString, String UID) throws ParseException {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        Query sQuery = new QueryParser("src", analyzer).parse(QueryParser.escape(queryString));
        BooleanClause sBooleanClause = new BooleanClause(sQuery, BooleanClause.Occur.MUST);
        queryBuilder.add(sBooleanClause);
        Query uQuery = new QueryParser("UID", analyzer).parse(UID);
        BooleanClause uidBooleanClause = new BooleanClause(uQuery, BooleanClause.Occur.MUST);
        queryBuilder.add(uidBooleanClause);
        BooleanQuery booleanQuery = queryBuilder.build();
        return booleanQuery;
    }

    public List<Document> searchSentence(Query query) throws IOException {
        HashMap<String, Document> documentHashMap = new HashMap<String, Document>();
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        TopDocs topDocs = searcher.search(query, NUMBER_OF_CONTEXT_CANDIDATES);

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = searcher.doc(scoreDoc.doc);
            String sentence = document.getField("src").toString();
            if (!documentHashMap.containsKey(sentence)) {
                documentHashMap.put(sentence, document);
            } else {
                long timeStamp1 = 0;
                timeStamp1 = document.getField("timeStamp").numericValue().longValue();
                Document document2 = documentHashMap.get(sentence);
                long timeStamp2 = document2.getField("timeStamp").numericValue().longValue();
                if (timeStamp1 > timeStamp2) {
                    documentHashMap.put(sentence, document);
                }
            }
        }
        List<Document> documents = new ArrayList(documentHashMap.values());
        return documents;

    }
}

