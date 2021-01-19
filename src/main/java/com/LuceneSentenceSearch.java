package com;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
import org.apache.lucene.document.*;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.codecs.lucene84.Lucene84Codec;


public class LuceneSentenceSearch {
    private static final int MAX_SENTENCE_LENGTH_BPE = 120;
    private static final int NUMBER_OF_CONTEXT_CANDIDATES = 1000;
    private final Directory indexDirectory;
    private final Analyzer analyzer;
    private static final String INSERT = "INSERT";
    private static final String GET = "GET";
    private static final String DELETE = "DELETE";
    private static final int CONTEXT_COUNT = 50;
    private float bleu_similarity_threshold = 0.05f;

    private static final Logger LOGGER = Logger.getLogger("Lucene translation memory.");

    private LuceneSentenceSearch(Directory fsDirectory, String language) {
        super();
        this.indexDirectory = fsDirectory;
        this.analyzer = AnalyzerContainer.getAnalyzer(language);
    }

    private static final Comparator<ScoreDoc> scoreDocComparator = new Comparator<ScoreDoc>() {
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

    public static LuceneSentenceSearch createIndex(){
        return createIndex("en");
    }

    public static LuceneSentenceSearch createIndex(String laguage) {
        String indexName = RandomStringUtils.randomAlphabetic(10);
        return createNamedIndex(indexName, laguage);
    }

    public static LuceneSentenceSearch createNamedIndex(String indexName, String laguage) {
        String indexDir = "index//" + indexName;
        LuceneSentenceSearch indexer = null;
        try {
            Path indexPath = Paths.get(indexDir);
            MMapDirectory fsDirectory = new MMapDirectory(indexPath);
            indexer = new LuceneSentenceSearch(fsDirectory, laguage);
        } catch (Exception ex) {
            System.out.println("Cannot create index..." + ex.getMessage());
            System.exit(-1);
        }
        return indexer;
    }

    public static void main(String[] args) {
        LuceneSentenceSearch indexer = createIndex();
        System.out.println("Ready:");
        Scanner sc = new Scanner(System.in);
        String command;
        String line;
        String UID = "";
        Query query;
        int i=0;
        while (sc.hasNextLine()) {
            System.out.println("Ready for input:");
            line = sc.nextLine();

            command = line.split("\t")[0];
            switch (command) {
                case GET:
                    UID = line.split("\t")[1];
                    String reference = line.split("\t")[2];

                    try {
                        reference = reference.replaceAll("@@ ", "");
                        List<Document> contextSentences = indexer.queryTM(reference, UID);
                        indexer.bleuRescorer(reference, contextSentences);
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
        indexWriterConfig.setCodec(new Lucene84Codec());
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

    public void addFileToIndex(String srcFile, String trgFile, String UID) throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setCodec(new Lucene84Codec());
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);

        Reader srcFileReader = new InputStreamReader(new FileInputStream(srcFile), StandardCharsets.UTF_8);
        BufferedReader srcFileBufferReader = new BufferedReader(srcFileReader);
        Reader trgFileReader = new InputStreamReader(new FileInputStream(trgFile), StandardCharsets.UTF_8);
        BufferedReader trgFileBufferReader = new BufferedReader(trgFileReader);

        String srcSent, trgSent;
        while ((srcSent=srcFileBufferReader.readLine())!=null) {
            trgSent=trgFileBufferReader.readLine();
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
        }
        trgFileBufferReader.close();
        srcFileBufferReader.close();
        indexWriter.close();
    }

    public void deleteSentenceFromIndexByUID(String UID) throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setCodec(new Lucene84Codec());
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
        Query UIDQuery = new TermQuery(new Term("UID", UID));
        indexWriter.deleteDocuments(UIDQuery);
        indexWriter.commit();
        indexWriter.close();
    }

    private ArrayList<Document> bleuRescorer(String reference, List<Document> candidates) {
        ArrayList<Document> contextSentences = new ArrayList<Document>();
        F1BleuCalculator calculator = new F1BleuCalculator(reference.split(" "));
        PriorityQueue<ScoreDoc> topScoringDocs = new PriorityQueue<ScoreDoc>(CONTEXT_COUNT, scoreDocComparator);
        for (int i = 0; i < candidates.size(); ++i) {
            Document hypothesisDoc = candidates.get(i);
            String hypothesis = hypothesisDoc.getField("srcBPE").stringValue();
            Float bleuScore = calculator.calc(hypothesis.split(" "));
            ScoreDoc scoredItem = new ScoreDoc(i, bleuScore);
            topScoringDocs.add(scoredItem);
        }
        int k = 0;
        while (!topScoringDocs.isEmpty() && k < CONTEXT_COUNT) {
            k += 1;
            ScoreDoc scoredItem = topScoringDocs.poll();
            if (scoredItem.score >= this.bleu_similarity_threshold) {
                contextSentences.add(candidates.get(scoredItem.doc));
            }
        }
        return contextSentences;
    }

    public List<Document> queryTM(String reference, String UID, boolean skipBleuRescorer, int numberOfCandidates) throws ParseException, IOException {
        String debpe_reference =  reference.replaceAll("@@ ", "");
        Query query = createQuery(debpe_reference, UID);
        List<Document> candidateSentences = searchSentence(query);
        if (skipBleuRescorer)
            return candidateSentences.subList(0, Integer.min(numberOfCandidates, NUMBER_OF_CONTEXT_CANDIDATES));
        else {
            List<Document> contextSentences = bleuRescorer(reference, candidateSentences);
            return contextSentences;
        }
    }

    public List<Document> queryTM(String reference, String UID, boolean skipBleuRescorer) throws ParseException, IOException {
        return queryTM(reference, UID, skipBleuRescorer, 50);
    }

    public List<Document> queryTM(String reference, String UID) throws ParseException, IOException {
        return queryTM(reference, UID, false, 50);
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

    public void setBleu_similarity_threshold(float bleu_similarity_threshold){
        this.bleu_similarity_threshold = bleu_similarity_threshold;
    }

}

