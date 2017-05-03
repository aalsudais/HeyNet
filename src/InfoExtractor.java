import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.TypedDependency;

public class InfoExtractor {

	private Map<String,String> keywords;
	private Map<String,String> namesMap;
	private Map<String, Integer> extraInfo; // 0 means no info is needed, 1 means info is needed
	
	public InfoExtractor() throws IOException{
		this("words.txt","hosts.txt");
	}
	
	public InfoExtractor(String wordsFile, String namesFile) throws IOException{
		FileReader wf, nf;
		BufferedReader wbr, nbr;
		
		wf = new FileReader(wordsFile);
		wbr = new BufferedReader(wf);
		nf = new FileReader(namesFile);
		nbr = new BufferedReader(nf);
		
		keywords = new HashMap<>();
		namesMap = new HashMap<>();
		extraInfo = new HashMap<>();
		
		String line;
		// reading the keyword list
		while ((line = wbr.readLine()) != null){
			String wordAndFlag [] = line.split(" ");
			keywords.put(wordAndFlag[0], wordAndFlag[0]);
			extraInfo.put(wordAndFlag[0], new Integer(wordAndFlag[1]));
		}
		// parsing names file
		while((line = nbr.readLine()) != null){
			String []split = line.split(" ");
			try{
				namesMap.put(split[0], split[1]);
			}catch(ArrayIndexOutOfBoundsException e){
				System.err.println("Error parsing names file.\nMake sure the format is like this for every line:\n<logicalName> <physicalName>");
			}
		}
		wbr.close();
		nbr.close();
		
		findSyns();
	}
	
	private void findSyns() throws IOException{
		String path = "/usr/local/WordNet-3.0/dict";
		URL url = new URL("file", null, path);
		IDictionary dict = new Dictionary(url);
		dict.open();
		Object[] keys = keywords.keySet().toArray();
		
		for (Object w: keys){
			IIndexWord idxWord = dict.getIndexWord(w.toString(), POS.VERB);
			IWordID wordID = idxWord.getWordIDs().get(0);
			IWord word = dict.getWord(wordID);
			for (IWord syn: word.getSynset().getWords()){
				keywords.put(syn.getLemma(),w.toString());
			}
		}
	}
	
	public AbstractTask getAbstractTask(String sentence) throws IOException{
		AbstractTask task = new AbstractTask();
		Sentence sent = new Sentence(sentence);
		List<String> lemmas = sent.lemmas();
		System.out.println();
		if (!assignKeyword(lemmas, task))
			return null; // i.e., couldn't locate keyword
		if (!assignType(lemmas, task))
			return null;
		assignDirEndpoints(lemmas, task, sent.dependencyGraph());
		if (needExtraInfo(task)){
			extractExtraInfo(task,sent.dependencyGraph());
		}
		System.out.println(sent.dependencyGraph()+"\n");
		return task;
	}
	
	
	private boolean assignKeyword(List<String> lemmas, AbstractTask task){
		for (String w: lemmas){
			if (keywords.containsKey(w.toLowerCase())){
				task.assignKeyword(w.toLowerCase());
				return true;
			}
		}
		return false;
	}
	
/*	private boolean assignEndpoints(List<String> lemmas, AbstractTask task){
		// TODO: write a method that determines the direction for the endpoints
		int count = 0;
		for (int i = 0; i < lemmas.size(); i++){
			String w = lemmas.get(i);
			if (namesMap.containsKey(w)){
				if (i != 0 && lemmas.get(i-1).equals("to"))
					task.addDstEndPoint(w.toLowerCase());
				else
					task.addSrcEndPoint(w.toLowerCase());
				count++;
			}
		}
		if (count > 0) // for now, every task has to have at least one endpoint
			return true;
		return false;
	}
*/	
	
	private void assignDirEndpoints (List<String> lemmas, AbstractTask task, SemanticGraph dep){
		//System.out.println(dep.toString());
		
		for (TypedDependency d: dep.typedDependencies()){
			String ep = d.dep().toString().split("/")[0].toLowerCase();
			if (namesMap.containsKey(ep) && task.isNewEndpoint(ep)){
				switch(d.reln().toString()){
				case "nmod:through":
				case "nmod:to":
				case "nmod:into":
					task.addDstEndPoint(ep);
					break;
				case "nmod:from":
					task.addSrcEndPoint(ep);
					break;
					
				default:
					task.addSrcEndPoint(ep);	
				}
			}
//			System.out.println(d+"---->\n"+d.gov()+"::::"+d.dep()+"::::"+d.reln());
		}
		
	}
	
	private boolean assignType(List<String> lemmas, AbstractTask task){
		if (lemmas.contains("?")){
			task.assignTaskType(AbstractTask.TaskType.GET);
			return true;
		}else if ((lemmas.contains("not")) || lemmas.contains("off")){
			task.assignTaskType(AbstractTask.TaskType.DELETE);
			return true;
		}else {
			task.assignTaskType(AbstractTask.TaskType.POST);
			return true;
		}
	}
	
	public int sizeOfAllKeywords(){
		return keywords.size();
	}
	
	public int sizeOfUniqueKeywords(){
		int count = 0;
		List<String> uniqueKeywords = new ArrayList<String>();
		for (String w: keywords.keySet()){
			String word = keywords.get(w);
			if (!uniqueKeywords.contains(word)){
				count ++;
				uniqueKeywords.add(word);
			}
		}
		return count;
	}

	public boolean needExtraInfo(AbstractTask task){
		/*
		 * Use this method to determine if more (extra) information is needed
		 */
		int flag = extraInfo.get(task.getKeyword());
		switch (flag){
		case 0:
			return false;
		case 1:
			return true;
		}
		return false;
	}
	
	public void extractExtraInfo(AbstractTask task, SemanticGraph dep){
		/*
		 * Use this method to extract the extra information, which is different for
		 * each keyword.
		 * E.g. if keyword is "limit", then we should look for numbers like (10mbps or 1Gbps)
		 */
		int flag = extraInfo.get(task.getKeyword());
		switch (flag){
		case 0:
			break;
		case 1:
			for (TypedDependency d: dep.typedDependencies()){
				String tag = d.dep().toString().split("/")[0].toLowerCase();
				if (d.reln().toString().equals("nummod")) // The bandwidth measurement unit should be the parent of this node
					task.addExtraInfo(tag);
				
			}
			break;
		default:
		}
		
	}
}
