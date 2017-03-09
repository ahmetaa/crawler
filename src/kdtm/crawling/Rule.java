package kdtm.crawling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by sila on 16.06.2016.
 */
public class Rule {
    Rule(){}

    String source;
    Set<String> ignores = new HashSet<>();

    ArrayList<Rule> getRules(String rules){
        ArrayList<Rule> ruleList = new ArrayList<>();
        Rule rule = new Rule();
        List<String> ignoreList;
        try (BufferedReader br = new BufferedReader(new FileReader(rules)))
        {
            int i=0;
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if(!sCurrentLine.equalsIgnoreCase("")){
                if (i % 2 == 0) {
                    rule = new Rule();
                    rule.source = sCurrentLine.substring(sCurrentLine.indexOf("=")+1).trim();
                }
                if (i % 2 == 1) {
                    sCurrentLine = sCurrentLine.substring(sCurrentLine.indexOf("=")+1).trim();
                    ignoreList = Arrays.asList(sCurrentLine.split(","));
                    rule.ignores = new HashSet<>(ignoreList);
                    ruleList.add(rule);
                }
                i++;
            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ruleList;
    }


}
