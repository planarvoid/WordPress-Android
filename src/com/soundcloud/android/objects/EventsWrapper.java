package com.soundcloud.android.objects;

import java.util.ArrayList;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class EventsWrapper  {

	private ArrayList<Event> collection;
	private String next_href;

	public void setCollection(ArrayList<Event> collection) {
		this.collection = collection;
	}

	public ArrayList<Event> getCollection() {
		return collection;
	}

	public void setNext_href(String next_href) {
		this.next_href = next_href;
	}

	public String getNext_href() {
		return next_href;
	}
	
	
}
