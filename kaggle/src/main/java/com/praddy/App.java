package com.praddy;


import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 */
public class App {

    static int TRAIN_FROM_SAMPLE = 30000;

    static int TRAIN_SAMPLE_EVALUATION_FROM = 10000;
    static int TRAIN_SAMPLE_EVALUATION_TO = 30000;

    static int ngram = 3;


    static POSModel model = new POSModelLoader()
            .load(new File("/Users/pradeepmishra/Downloads/en-pos-maxent.bin"));
    static POSTaggerME tagger = new POSTaggerME(model);

    //Data Containers

    // Great =4 Poor=0 Range is 0-4
    static HashMap<String, Integer> wordWeight = new HashMap<String, Integer>();
    static HashMap<String, Frequency> wordWeightFrequency = new HashMap<String, Frequency>();


    // Noun[0],Adj[4] = 3, Adj[4]Adv[3] = 5 . what do 2 phrases generally contribute
    static HashMap<String, Integer> wordTypeNGramContribution = new HashMap<String, Integer>();
    static HashMap<String, Frequency> wordTypeNGramContributionFrequency = new HashMap<String, Frequency>();


    public static void main(String[] args) {
        loadData();
        testReviews();
    }

    public static void testReviews() {
        String csvFile = "/Users/pradeepmishra/datascience/kaggle/train.tsv";
        BufferedReader br = null;
        String line = "";
        String tabSplitBy = "\t";

        int successCount = 0;
        int failureCount = 0;
        int errorCount = 0;
        int relativeSuccessCount = 0;


        try {

            int count = 0;

            int PhraseId = 0, SentenceId = 1, Phrase = 2, Sentiment = 3;

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null && count++ < TRAIN_SAMPLE_EVALUATION_TO) {
                if (count < TRAIN_SAMPLE_EVALUATION_FROM) {
                    //Ignore heading line
                    continue;
                }
                // use comma as separator
                String[] row = line.split(tabSplitBy);
                String phrase = row[Phrase];
                String[] words = phrase.split("\\s+");


                if (null == words || words.length == 0 || !StringUtils.isNumeric(row[Sentiment])) {
                    errorCount++;
                    continue;
                }

                Integer sentiment = Integer.parseInt(row[Sentiment]);

                Integer wordbasedweightage = 0;
                Integer wordNgrambasedweightage = 0;
                //word based weighatge
//                String prevWord = null;
//                String prevWordType = null;

                int index = -1;
                for (String word : words) {
                    index++;
                    Integer wordWeightage = wordWeight.get(word);
                    if (null != wordWeightage) {
                        wordbasedweightage += wordWeightage;
                    } else {
                        wordbasedweightage += 2;
                    }
//                    if (null == prevWordType) {
//                        prevWord = word;
//                        prevWordType = getWordType(word);
//                        continue;
//                    }
                    String currWord = word;
                    String currWordType = getWordType(word);

//                    String KEY = prevWord + "[" + prevWordType + "]" + currWord + "[" + currWordType + "]";
                    String KEY = "";

                    int currGram = 0;

                    boolean notPossible = false;
                    while (currGram++ < ngram) {
                        if (index - currGram < 0) {
                            notPossible = true;
                            break;
                        }
                        KEY = getWordType(words[index - currGram]) + "[" + wordWeight.get(words[index - currGram]) + "]" + KEY;
                    }
                    if (notPossible) {
                        continue;
                    }

                    Integer wordTypeNGramContributionWeightage = wordTypeNGramContribution.get(KEY);
                    if (null != wordTypeNGramContributionWeightage) {
                        wordNgrambasedweightage += wordTypeNGramContributionWeightage;
                    } else {
                        wordNgrambasedweightage += 2;
                    }

//                    prevWord = currWord;
//                    prevWordType = currWordType;
                }

                Integer wordBasedWeightage = Math.round(wordbasedweightage / words.length);
                Integer finalWeightage = wordBasedWeightage;
                Integer word2GramBasedWeightage = 2;

                if (words.length - ngram + 1 > 0) {
                    word2GramBasedWeightage = Math.round(wordNgrambasedweightage / (words.length - ngram + 1));
                    finalWeightage = Math.round((wordBasedWeightage + word2GramBasedWeightage) / 2);
                }


                if (finalWeightage == sentiment) {
                    successCount++;
                } else {
                    failureCount++;
                }
                if (Math.abs(finalWeightage - sentiment) <= 1.0) {
                    relativeSuccessCount++;
                }


            }
            System.out.println("Total Successes : " + successCount);
            System.out.println("Total Failures : " + failureCount);
            System.out.println("Total Errors : " + errorCount);
            System.out.println("Relative Successes : " + relativeSuccessCount);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void loadData() {
        String csvFile = "/Users/pradeepmishra/datascience/kaggle/train.tsv";
        BufferedReader br = null;
        String line = "";
        String tabSplitBy = "\t";


        try {

            int count = 0;

            int PhraseId = 0, SentenceId = 1, Phrase = 2, Sentiment = 3;

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null && count++ < TRAIN_FROM_SAMPLE) {
                if (count < 2) {
                    //Ignore heading line
                    continue;
                }
                // use comma as separator
                String[] row = line.split(tabSplitBy);
                String phrase = row[Phrase];
                String[] words = phrase.split("\\s+");
                Integer sentiment = Integer.parseInt(row[Sentiment]);

                //word based
                for (String word : words) {
                    Frequency freq = wordWeightFrequency.get(word);
                    if (null == freq) {
                        freq = new Frequency();
                    }
                    HashMap<Integer, Integer> keyValue = freq.getKeyValue();
                    if (null == keyValue) {
                        keyValue = new HashMap<Integer, Integer>();
                        keyValue.put(sentiment, 0);
                    }
                    Integer value = keyValue.get(sentiment);
                    if (null == value) {
                        value = 0;
                    }
                    keyValue.put(sentiment, value + 1);
                    freq.setKeyValue(keyValue);
                    wordWeightFrequency.put(word, freq);

                }
            }

//            System.out.println("wordWeightFrequency:" + wordWeightFrequency);

            br.close();

            for (Map.Entry<String, Frequency> entry : wordWeightFrequency.entrySet()) {
                wordWeight.put(entry.getKey(), 2);
                int maxFrequency = 0;
                for (Map.Entry<Integer, Integer> levelFrequency : entry.getValue().getKeyValue().entrySet()) {
                    if (levelFrequency.getValue() > maxFrequency) {
                        maxFrequency = levelFrequency.getValue();
                        wordWeight.put(entry.getKey(), levelFrequency.getKey());
                    }
                }
            }

            br = new BufferedReader(new FileReader(csvFile));

            count = 0;
            //Go through the file once more to get phrases
            while ((line = br.readLine()) != null && count++ < TRAIN_FROM_SAMPLE) {

                if (count < 2) {
                    continue;
                }

                // use comma as separator
                String[] row = line.split(tabSplitBy);
                String phrase = row[Phrase];
                String[] words = phrase.split("\\s+");
                Integer sentiment = Integer.parseInt(row[Sentiment]);

                //word phrase (2 gram) based
//                String prevWordType = null;
//                String prevWord = null;
                int index = -1;
                for (String word : words) {
                    index++;
//                    if (null == prevWordType) {
//                        prevWordType = getWordType(word);
//                        prevWord = word;
//                        continue;
//                    }
                    String currentWordType = getWordType(word);
                    String currentWord = word;
                    //wordtype1[sentiment]wordtype2[sentiment] = 3
                    String KEY = "";
                    int currGram = 0;

                    boolean notPossible = false;
                    while (currGram++ < ngram) {
                        if (index - currGram < 0) {
                            notPossible = true;
                            break;
                        }
                        KEY = getWordType(words[index - currGram]) + "[" + wordWeight.get(words[index - currGram]) + "]" + KEY;
                    }
                    if (notPossible) {
                        continue;
                    }

//                            prevWordType + "[" + wordWeight.get(prevWord) + "]" + currentWordType + "[" + wordWeight.get(currentWord) + "]";
                    Frequency freq = wordTypeNGramContributionFrequency.get(KEY);
                    if (null == freq) {
                        freq = new Frequency();
                    }
                    HashMap<Integer, Integer> keyValue = freq.getKeyValue();
                    if (null == keyValue) {
                        keyValue = new HashMap<Integer, Integer>();
                        keyValue.put(sentiment, 0);
                    }
                    Integer value = keyValue.get(sentiment);
                    if (null == value) {
                        value = 0;
                    }
                    keyValue.put(sentiment, value + 1);
                    freq.setKeyValue(keyValue);
                    wordTypeNGramContributionFrequency.put(KEY, freq);
//                    prevWordType = currentWordType;
                }
            }


            for (Map.Entry<String, Frequency> entry : wordTypeNGramContributionFrequency.entrySet()) {
                wordTypeNGramContribution.put(entry.getKey(), 2);
                int maxFrequency = 0;
                for (Map.Entry<Integer, Integer> levelFrequency : entry.getValue().getKeyValue().entrySet()) {
                    if (levelFrequency.getValue() > maxFrequency) {
                        maxFrequency = levelFrequency.getValue();
                        wordTypeNGramContribution.put(entry.getKey(), levelFrequency.getKey());
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("wordWeight:" + wordWeight);
        System.out.println("wordTypeNGramContribution:" + wordTypeNGramContribution);
        System.out.println("Done");
    }

    public static String getWordType(String input) throws IOException {

//        String input = "Hi. How are you? This is Mike.";
        ObjectStream<String> lineStream = new PlainTextByLineStream(
                new StringReader(input));

        String line;
        while ((line = lineStream.read()) != null) {
            String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE
                    .tokenize(line);
            String[] tags = tagger.tag(whitespaceTokenizerLine);

            POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
//            System.out.println(sample.toString());
            String removeWordFromType = sample.toString().replace(input, "");
            return removeWordFromType;
        }

        return "NA";
    }
}
