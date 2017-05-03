import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class AbstractTask {

	public enum TaskType{
		GET,POST,DELETE
	}
	
	private ArrayList<ArrayList<String>> endpoints;
	private String keyword;
	private String extraInfo;
	private TaskType type;

	public AbstractTask(){
		endpoints = new ArrayList<ArrayList<String>>();
		endpoints.add(new ArrayList<String>());
		endpoints.add(new ArrayList<String>());
		keyword = new String();
		extraInfo = new String();
		type = TaskType.GET;
	}
		
	public void addSrcEndPoint(String e){
		endpoints.get(0).add(e);
	}
	
	public void addDstEndPoint(String e){
		endpoints.get(1).add(e);
	}
	
	public void assignKeyword(String k){
		keyword = k;
	}
	
	public String getKeyword(){
		return keyword;
	}
	
	public void addExtraInfo(String i){
		extraInfo = extraInfo.concat(i);
	}
	
	public void assignTaskType(TaskType t){
		type = t;
	}
	
	public boolean isNewEndpoint(String e){
		if (endpoints.get(0).contains(e) || endpoints.get(1).contains(e))
			return false;
		return true;
	}
	
	public String serialize(){
		JsonArrayBuilder src = Json.createArrayBuilder();
		for (int i = 0; i<endpoints.get(0).size(); i++){
			src.add(endpoints.get(0).get(i));
		}
		JsonArrayBuilder dst = Json.createArrayBuilder();
		for (int i = 0; i<endpoints.get(1).size(); i++){
			dst.add(endpoints.get(1).get(i));
		}
		JsonObject ends = Json.createObjectBuilder().add("src", src).add("dst", dst).build();
		JsonObject jsonObject =
		        Json.createObjectBuilder()
		                .add("keyword", keyword)
		                .add("endpoints", ends)
		                .add("type", type.toString())
		                .add("extraInfo", extraInfo)
		        .build();
		return jsonObject.toString();
		
	}
	
	@Override
	public String toString(){
		return "keyword:"+keyword + ";endpoints:"+endpoints.toString() + ";type:"+type + ";extraInfo:"+extraInfo;
	}
}
