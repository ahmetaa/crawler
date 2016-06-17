package kdtm.crawling;


import java.util.ArrayList;
/**
 * Created by sila on 16.06.2016.
 */
public class ExtractRules {
    public static void main(String[] args) {
        Rule rules = new kdtm.crawling.Rule();
        int c =0;
        ArrayList ruleList =  rules.getRules("/home/sila/projects/crawler/domains/rules");
        rules = (kdtm.crawling.Rule) ruleList.get(0);



        String href = "http%3A%2F%2Fwww.nurturia.com.tr%2Fbazaar%2Fe7ae55bd-88d9-48d5-8415-9cf400ca12d9%2F3%2Fikinci-el   ";
//        System.out.println(ruleList.size());
        for (int i = 0; i < ruleList.size(); i++) {
            rules = (kdtm.crawling.Rule) ruleList.get(i);
            String s = rules.source.replace(":", "%3A").replace("/", "%2F");
            if(href.contains(rules.source) || href.contains(rules.source.replace(":", "%3A").replace("/", "%2F"))) {
                for(String ig : rules.ignores) {
                    if(href.contains(ig))
                        c++;

                }
            }

            if(c!=0)
                System.out.println("!!!");
            else
                System.out.println(href);
        }



//        for(String ig : rules.ignores) {
//            System.out.println(ig);
//        }
//        System.out.println(rules.source);
    }
}
