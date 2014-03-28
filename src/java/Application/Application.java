package Application;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import org.json.JSONObject;

/**
 *
 * @author martinfelcman
 */
@Named(value = "Application")
@SessionScoped
public class Application implements Serializable {

    private String pageID = "5550296508";
    private String accesstoken = "CAACEdEose0cBACVIFiHWZCJkilpc5t9kbmAD5jnwlBhbF9yFNiXGQPZCrnNJzSe56LBrssgavKuMpTajVKZB7iwAE0N0n3vF7M63xUZAiW2OaZCG5XcQVXTuqOuP4QRBlRIcNG4TfUtWIglSBn9xZAc88QqvDnFSHQfBbPlqnyEO4HrpnaZCyqSkTRvBadbuuIZD";
    private String limit = "2000";
    
    private String result;
    private ArrayList keywords;
    
    private HashMap<String, Integer> countries;
    private HashMap<String, Integer> organizations;
    private HashMap<String, Integer> cities;
    private HashMap<String, Integer> people;

    private final String syntaxPath = "/Users/martinfelcman/Downloads/DDW-HW1-felcmma1/syntax.jape";
    private static SerialAnalyserController annotationPipeline = null;
    private static boolean isGateInitilised = false;
    private Document document1;

    public void setPageID(String id) {
        this.pageID = id;
    }

    public void setAccesstoken(String token) {
        this.accesstoken = token;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getPageID() {
        return this.pageID;
    }

    public String getAccesstoken() {
        return this.accesstoken;
    }

    public String getLimit() {
        return this.limit;
    }

    public void setResult(String r) {
        this.result = r;
    }

    public String getResult() {
        return this.result;
    }

    public void setKeywords() {
        this.keywords = new ArrayList();
    }

    public ArrayList getKeywords() {
        return this.keywords;
    }

    public void setCountries() {
        this.countries = new HashMap<String, Integer>();
    }

    public HashMap<String, Integer> getCountries() {
        return this.countries;
    }

    public void setOrganizations() {
        this.organizations = new HashMap<String, Integer>();
    }

    public HashMap<String, Integer> getOrganizations() {
        return this.organizations;
    }

    public void setCities() {
        this.cities = new HashMap<String, Integer>();
    }

    public HashMap<String, Integer> getCities() {
        return this.cities;
    }

    public void setPeople() {
        this.people = new HashMap<String, Integer>();
    }

    public HashMap<String, Integer> getPeople() {
        return this.people;
    }


    public void process() throws GateException {

        this.setPageID(this.pageID);
        this.setAccesstoken(this.accesstoken);
        this.setLimit(this.limit);
        
        this.setKeywords();
        this.setResult("");

        this.getFBdata();


        this.run();
    }

    public void run() throws GateException {
        if (!isGateInitilised) {
            initialiseGate();
        }
        try {
            ProcessingResource annieGazeter = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
            // create an instance of a Document Reset processing resource
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");
            // locate the JAPE grammar file
            File japeOrigFile = new File(this.syntaxPath);
            java.net.URI japeURI = japeOrigFile.toURI();
            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }
            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
            // add the processing resources (modules) to the pipeline
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(annieGazeter);
            //   annotationPipeline.add(neTransduser);
            annotationPipeline.add(japeTransducerPR);

            this.document1 = Factory.newDocument(this.result);


            // create a corpus and add the document
            Corpus corpus = Factory.newCorpus("");
            corpus.add(this.document1);
            // set the corpus to the pipeline
            annotationPipeline.setCorpus(corpus);
            //run the pipeline
            annotationPipeline.execute();

            for (int i = 0; i < corpus.size(); i++) {
                Document document = corpus.get(i);
                // get the default annotation set
                AnnotationSet as_default = document.getAnnotations();
                FeatureMap futureMap = null;


                AnnotationSet annsetCountries = as_default.get("Country", futureMap);
                AnnotationSet annsetOrganizations = as_default.get("Organization", futureMap);
                AnnotationSet annsetCities = as_default.get("City", futureMap);
                AnnotationSet annsetPersons = as_default.get("Person_full", futureMap);


                this.keywords = new ArrayList(as_default);
                // looop through the Token annotations

                processTokensInArrayList(this.keywords, document);
                
                
                this.countries= processHashMap(processTokensInArrayList(new ArrayList(annsetCountries), document));
                this.organizations = processHashMap(processTokensInArrayList(new ArrayList(annsetOrganizations), document));
                this.cities  = processHashMap(processTokensInArrayList(new ArrayList(annsetCities), document));
                this.people = processHashMap(processTokensInArrayList(new ArrayList(annsetPersons), document));
                
                
                
                
                

            }
        } catch (GateException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initialiseGate() throws GateException {
        try {
            // set GATE home folder
            File gateHomeFile = new File("/Applications/GATE_Developer_7.1");
            Gate.setGateHome(gateHomeFile);

            // set GATE plugins folder          
            File pluginsHome = new File("/Applications/GATE_Developer_7.1/plugins");
            Gate.setPluginsHome(pluginsHome);

            // initialise the GATE library
            Gate.init();

            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);

            // flag that GATE was successfuly initialised
            isGateInitilised = true;

        } catch (MalformedURLException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GateException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void getFBdata() {


        try {
            URL url = new URL("https://graph.facebook.com/" + pageID + "/feed/?fields=message&limit=" + limit + "&access_token=" + accesstoken);
            URLConnection connection = url.openConnection();

            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject json = new JSONObject(builder.toString());
            this.result = json.toString();

            Iterator it = json.keys();

            while (it.hasNext()) {
                System.out.println(it.next().toString());
            }




//            JSONArray results = json.getJSONArray("message");
//            for (int i = 0; i < results.length(); i++)  {
//                this.keywords.add(results.optString(i));
//          
//            }

        } catch (Exception e) {
            System.out.println("Error");
        }

    }

    private ArrayList processTokensInArrayList(ArrayList a, Document document) throws InvalidOffsetException {
        ArrayList tmp = new ArrayList(a);
        a.clear();

        for (int j = 0; j < tmp.size(); ++j) {
            // get a token annotation

            Annotation token = (Annotation) (tmp.get(j));

            // get the underlying string for the Token
            Node isaStart = token.getStartNode();
            Node isaEnd = token.getEndNode();
            String resultValue = document.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
            a.add(resultValue);
        }
        return a;
    }

    private HashMap<String, Integer> processHashMap(ArrayList array) {
       
        
                HashMap<String, Integer> tmp = new HashMap<String, Integer>();

                for (int k = 0; k < array.size(); k++) {
                    tmp.put(array.get(k).toString(), 0);
                }
                for (int l = 0; l < array.size(); l++) {
                    int count = tmp.get(array.get(l).toString());
                    tmp.put(array.get(l).toString(), ++count);
                }
                
                return tmp;
        
        
    }
}
