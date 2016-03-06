/**
 * 
 */
package fr.nikk.jdb.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lightcouch.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.nikk.model.note.Note;
import fr.nikk.services.couchdb.repository.NoteDAO;
import fr.nikk.services.couchdb.repository.UnimplementedOperationException;

/**
 * @author Alexandre Guyon
 *
 */
public class ContentNoteController extends HttpServlet implements Controller {

	private static final long serialVersionUID = -3272120673430017122L;

	private NoteDAO dao;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println("coucou!");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println(req.getRequestURI());
		if(req.getParameterMap().containsKey("note") && req.getParameterMap().containsKey("tag")){
			String note = req.getParameter("note");
			String tag = req.getParameter("tag");
			if(req.getParameterMap().containsKey("rev")){ // modification of existing note
				String id = req.getParameter("id");
				String rev = req.getParameter("rev");
				
				resp.getWriter().println(this.modNote(id, rev, tag, note));
			}else // Add new note
				resp.getWriter().println(this.addNote(tag, note));
		}
		
		resp.setContentType("text/json;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Add a note
	 * @param tag Tag, comma separated
	 * @param note Note
	 * @return The new string
	 */
	@POST
	@Path("/{tag}/{note}")
	public String addNote(@PathParam("tag") String tag, @PathParam("note") String note){
		Note n = new Note();
		n.setNote(note);
		n.setTag(tag);
		try {
			Response r = this.dao.save(n);
			n.set_id(r.getId());
			n.set_rev(r.getRev());
			
			String ret = "";
			try {
				ret = this.mapper.writeValueAsString(n);
			} catch (JsonProcessingException e) {
				ret = "jdb error :\n";
				ret += e.getMessage();
			}
			return ret;
		} catch (UnimplementedOperationException e1) {
			e1.printStackTrace();	
			return "";
		}
	}
	
	/**
	 * Add a note
	 * @param tag Tag, comma separated
	 * @param note Note
	 * @param d Note's date
	 * @return The new string
	 */
	@POST
	@Path("/{tag}/{note}/{date}")
	public String addDateNote(@PathParam("tag") String tag, @PathParam("note") String note, Date d){
		Note n = new Note();
		n.setNote(note);
		n.setTag(tag);
		n.setDate(d);
		try {
			this.dao.save(n);
			
			String ret = "";
			try {
				ret = this.mapper.writeValueAsString(n);
			} catch (JsonProcessingException e) {
				ret = "jdb error :\n";
				ret += e.getMessage();
			}
			return ret;
		} catch (UnimplementedOperationException e1) {
			e1.printStackTrace();	
			return "";
		}
	}
	
	/**
	 * Modify note
	 * @param id Id of the note to delete
	 * @param rev Rev of the object to delete
	 * @param tag Tag, comma separated
	 * @param note Note
	 * @return status json
	 */
	@POST
	@Path("/{id}/{rev}/{tag}/{note}")
	public String modNote(@PathParam("id")String id, @PathParam("rev") String rev, @PathParam("tag") String tag, @PathParam("note") String note){
		DateFormat df = DateFormat.getDateInstance();
		
		Note n = new Note();
		n.set_id(id);
		n.set_rev(rev);
		n.setTag(tag);
		n.setNote(note);
		List<Date> d = new ArrayList<>();
		
		this.dao.getRevisions(n);
		
		JSONObject js = new JSONObject(this.dao.getByIdAndRev(id, rev));
		if(js.has("dateModif")){
			JSONArray dates = js.getJSONArray("dateModif");			
			
			for (int i= 0 ; i < dates.length() ; ++i){
				try {
					d.add(df.parse(dates.getString(i)));
				} catch (JSONException | ParseException e) {
					e.printStackTrace();
				}
			}
		}
		
		d.add(new Date());
		n.setDateModif(d);
		
		if(js.has("date")){
			try {
				n.setDate(df.parse(js.getString("date")));
			} catch (JSONException | ParseException e1) {
				e1.printStackTrace();
			}
		}else
			return "{\"status\" : \"no creation date in database\"}";

		try {
			Response r = this.dao.update(n);
			n.set_rev(r.getRev());
			return this.mapper.writeValueAsString(n);
		} catch (UnimplementedOperationException | JsonProcessingException e) {
			e.printStackTrace();
			return "{\"status\" : \"error\"}";
		}
	}
	
	/**
	 * @param dao the dao to set
	 */
	public void setDao(NoteDAO dao) {
		this.dao = dao;
	}

}