package org.addhen.ushahidi;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
 
import org.addhen.ushahidi.net.Categories;
import org.addhen.ushahidi.net.Incidents;
import org.addhen.ushahidi.data.CategoriesData;
import org.addhen.ushahidi.data.HandleXml;
import org.addhen.ushahidi.data.IncidentsData;
import org.addhen.ushahidi.data.UshahidiDatabase;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
 
public class ListIncidents extends Activity
{
  
  /** Called when the activity is first created. */
  private ListView listIncidents = null;
  private ListIncidentAdapter ila = new ListIncidentAdapter( this );
  private static final int HOME = Menu.FIRST+1;
  private static final int ADD_INCIDENT = Menu.FIRST+2;
  private static final int INCIDENT_MAP = Menu.FIRST+3;
  private static final int INCIDENT_REFRESH= Menu.FIRST+4;
  private static final int SETTINGS = Menu.FIRST+5;
  private static final int ABOUT = Menu.FIRST+6;
  private static final int GOTOHOME = 0;
  private static final int VIEW_INCIDENT = 0;
  private static final int USHAHIDI = 1;
  private static final int DIALOG_NETWORK_ERROR = 1;
  private static final int DIALOG_LOADING_INCIDENTS = 2;
  private static final int DIALOG_EMPTY_INCIDENTS = 3;
  private static final int LIST_INCIDENTS = 0;
  private Spinner spinner = null;
  private ArrayAdapter<String> spinnerArrayAdapter;
  private Bundle incidentsBundle = new Bundle();
  private final Handler mHandler = new Handler();
  private static final String TAG = "ListIncidents";
  public static UshahidiDatabase mDb;
  
  private List<IncidentsData> mNewIncidents;
  private List<IncidentsData> mOldIncidents;
  private List<CategoriesData> mNewCategories;
  private Vector<String> vectorCategories = new Vector<String>();
  
  public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	  setContentView( R.layout.list_incidents );
       
	  listIncidents = (ListView) findViewById( R.id.view_incidents );
        
	  mDb = new UshahidiDatabase(this);
	  mDb.open();
	  mOldIncidents = new ArrayList<IncidentsData>();
	  listIncidents.setOnItemClickListener( new OnItemClickListener(){  
      
		  public void onItemClick(AdapterView<?> arg0, View view, int position,
        		  long id) {
        	 
        	  incidentsBundle.putString("title",mOldIncidents.get(position).getIncidentTitle());
        	  incidentsBundle.putString("desc", mOldIncidents.get(position).getIncidentDesc());
        	  incidentsBundle.putString("category", mOldIncidents.get(position).getIncidentCategories());
        	  incidentsBundle.putString("location", mOldIncidents.get(position).getIncidentLocation());
        	  incidentsBundle.putString("date", mOldIncidents.get(position).getIncidentDate());
        	  incidentsBundle.putString("media", mOldIncidents.get(position).getIncidentMedia());
        	  incidentsBundle.putString("status", ""+mOldIncidents.get(position).getIncidentVerified());
          
        	  Intent intent = new Intent( ListIncidents.this,ViewIncidents.class);
				intent.putExtra("incidents", incidentsBundle);
				startActivityForResult(intent,VIEW_INCIDENT);
				setResult( RESULT_OK, intent );
              finish();
          }
          
      });
      spinner = (Spinner) findViewById(R.id.incident_cat);
        
      //mHandler.post(mDisplayIncidents);
	  //mark all incidents as read
      //mDb.markAllIncidentssRead();
  }
  
  protected void onResume(){
	  mHandler.post(mDisplayIncidents);
	  //mark all incidents as read
      mDb.markAllIncidentssRead();
	  super.onResume();
  }
  
  public void onDestory() {
	  mDb.close();
	  super.onDestroy();
  }
  
  
  
  private void retrieveIncidentsAndCategories() {
	  mHandler.post(mRetrieveNewIncidents);
	 // final Thread tr = new Thread() {
		//  public void run() {
			  
		 // }
	  //};
	  //tr.start();
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_NETWORK_ERROR: {
                AlertDialog dialog = (new AlertDialog.Builder(this)).create();
                dialog.setTitle("Network error!");
                dialog.setMessage("Network error, please ensure you are connected to the internet");
                dialog.setButton2("Ok", new Dialog.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            Intent intent = new Intent( ListIncidents.this,Ushahidi.class);
            startActivityForResult( intent,USHAHIDI );
            setResult( RESULT_OK );
     finish();
          }
            });
                dialog.setCancelable(false);
                return dialog;
            }
            
            case DIALOG_LOADING_INCIDENTS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Loading incidents");
                dialog.setMessage("Please wait while incidents are loaded...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                return dialog;
            }
            
            case DIALOG_EMPTY_INCIDENTS: {
              AlertDialog dialog = (new AlertDialog.Builder(this)).create();
              dialog.setTitle("No incidents!");
              dialog.setMessage("No incident available for this category, please select " +
                  "a new category to filter by.");
              dialog.setButton2("Ok", new Dialog.OnClickListener() {
                public void onClick( DialogInterface dialog, int which ) {
                  dialog.dismiss();
                }
              });
              dialog.setCancelable(false);
                return dialog;
            }
            
        }
        return null;
    }
  
  final Runnable mDisplayIncidents = new Runnable() {
    public void run() {
    	setProgressBarIndeterminateVisibility(true);
    	showIncidents("All");
    	showCategories();
      try{
    	  setProgressBarIndeterminateVisibility(false);
      } catch(Exception e){
        return;  //means that the dialog is not showing, ignore please!
      }
    }
  };
  
  final Runnable mDisplayCategories = new Runnable() {
    public void run() {
      showCategories();
      try{
        //dismissDialog( DIALOG_LOADING_INCIDENTS );
      } catch(Exception e){
        return;  //means that the dialog is not showing, ignore please!
      }
    }
  };
  
  final Runnable mDisplayNetworkError = new Runnable(){
    public void run(){
      showDialog(DIALOG_NETWORK_ERROR);
    }
  };
  
  final Runnable mDisplayIncidentsLoading = new Runnable() {
    public void run() {
      showDialog(DIALOG_LOADING_INCIDENTS);
    }
  };
  
  final Runnable mDisplayEmptyIncident = new Runnable() {
    public void run() {
      showDialog(DIALOG_EMPTY_INCIDENTS);
    }
  };
  
  final Runnable mDismissLoading = new Runnable(){
    public void run(){
      try{
        dismissDialog(DIALOG_LOADING_INCIDENTS);        
      } catch(IllegalArgumentException e){
        return;  //means that the dialog is not showing, ignore please!
      }
    }
  };
  
  final Runnable mRetrieveNewIncidents = new Runnable() {
	  public void run() {
	  try {
		  if( Util.isInternetConnection(ListIncidents.this)) {
			  setProgressBarIndeterminateVisibility(true);
			  if(Incidents.getAllIncidentsFromWeb()){
				  mNewIncidents =  HandleXml.processIncidentsXml( UshahidiService.incidentsResponse ); 
			  }
	   
			  if(Categories.getAllCategoriesFromWeb() ) {
				  mNewCategories = HandleXml.processCategoriesXml(UshahidiService.categoriesResponse);
			  }
			  mDb.addIncidents(mNewIncidents, false);
	    	
	  			  mDb.addCategories(mNewCategories, false);
	  			  setProgressBarIndeterminateVisibility(false);
		  } else {
			  Toast.makeText(ListIncidents.this, R.string.internet_connection, Toast.LENGTH_LONG);
		  }
	  	} catch (IOException e) {
					//means there was a problem getting it
	  	}
	  }
  };

 
  //menu stuff
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
    populateMenu(menu);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    populateMenu(menu);
 
    return(super.onCreateOptionsMenu(menu));
  }
 
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    applyMenuChoice(item);
 
    return(applyMenuChoice(item) ||
        super.onOptionsItemSelected(item));
  }
 
  public boolean onContextItemSelected(MenuItem item) {
 
    return(applyMenuChoice(item) ||
        super.onContextItemSelected(item));
  }
  
  private void populateMenu(Menu menu) {
	  MenuItem i;i = menu.add( Menu.NONE, HOME, Menu.NONE, R.string.menu_home );
		i.setIcon(R.drawable.ushahidi_home);
		
		i = menu.add( Menu.NONE, ADD_INCIDENT, Menu.NONE, R.string.incident_menu_add);
		i.setIcon(R.drawable.ushahidi_add);
		  
		i = menu.add( Menu.NONE, INCIDENT_MAP, Menu.NONE, R.string.incident_menu_map );
		i.setIcon(R.drawable.ushahidi_map);
		  
		
		i = menu.add( Menu.NONE, INCIDENT_REFRESH, Menu.NONE, R.string.incident_menu_refresh );
		i.setIcon(R.drawable.ushahidi_refresh);
		  
		i = menu.add( Menu.NONE, SETTINGS, Menu.NONE, R.string.menu_settings );
		i.setIcon(R.drawable.ushahidi_settings);
		  
		i = menu.add( Menu.NONE, ABOUT, Menu.NONE, R.string.menu_about );
		i.setIcon(R.drawable.ushahidi_settings);
	  
  }
  
  private boolean applyMenuChoice(MenuItem item) {
    switch (item.getItemId()) {
      case INCIDENT_REFRESH:
    	  retrieveIncidentsAndCategories();
        return(true);
    
      case INCIDENT_MAP:
        //TODO
        return(true);
    
      case ADD_INCIDENT:
        //TODO
        return(true);
        
      case SETTINGS:
    	  return(true);
        
    }
    return(false);
  }
  
  // get incidents from the db
  public void showIncidents( String by ) {
    
	  Cursor cursor;
	  if( by.equals("All")) 
		  cursor = mDb.fetchAllIncidents();
	  else
		  cursor = mDb.fetchIncidentsByCategories(by);
	  
	  String title;
	  String status;
	  String date;
	  String description;
	  String location;
	  String categories;
	  String media;
	
	  String thumbnails [];
	  Drawable d = null;
	  if (cursor.moveToFirst()) {
		  int idIndex = cursor.getColumnIndexOrThrow( 
				  UshahidiDatabase.INCIDENT_ID);
		  int titleIndex = cursor.getColumnIndexOrThrow(
				  UshahidiDatabase.INCIDENT_TITLE);
		  int dateIndex = cursor.getColumnIndexOrThrow(
				  UshahidiDatabase.INCIDENT_DATE);
		  int verifiedIndex = cursor.getColumnIndexOrThrow(
				  UshahidiDatabase.INCIDENT_VERIFIED);
		  int locationIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_LOC_NAME);
		  
		  int descIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_DESC);
		  
		  int categoryIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_CATEGORIES);
		  
		  int mediaIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_MEDIA);
		  
		  ila.removeItems();
		  
		  do {
			  
			  IncidentsData incidentData = new IncidentsData();
			  mOldIncidents.add( incidentData );
			  
			  int id = Util.toInt(cursor.getString(idIndex));
			  incidentData.setIncidentId(id);
			  
			  title = Util.capitalizeString(cursor.getString(titleIndex));
			  incidentData.setIncidentTitle(title);
			  
			  description = cursor.getString(descIndex);
			  incidentData.setIncidentDesc(description);
			  
			  categories = cursor.getString(categoryIndex);
			  incidentData.setIncidentCategories(categories);
			  
			  location = cursor.getString(locationIndex);
			  incidentData.setIncidentLocLongitude(location);
			  
			  date = Util.joinString("Date: ",cursor.getString(dateIndex));
			  incidentData.setIncidentDate(cursor.getString(dateIndex));			  
			  
			  media = cursor.getString(mediaIndex);
			  incidentData.setIncidentMedia(media);
			  thumbnails = media.split(",");
			  
			  //TODO make the string readable from the string resource
			  status = Util.toInt(cursor.getString(verifiedIndex) ) == 0 ? "Unverified" : "Verified";
			  incidentData.setIncidentVerified(Util.toInt(cursor.getString(verifiedIndex) ));
			  
			  //TODO do a proper check of thumbnails
			  d = ImageManager.getImages( thumbnails[0]);
			  
			  ila.addItem( new ListIncidentText( d == null ? getResources().getDrawable( R.drawable.ushahidi_icon):d, 
					  title, date, 
					  	status,description,location,media,categories, id) );
			  
		  } while (cursor.moveToNext());
	  }
    
	  cursor.close();
	  listIncidents.setAdapter( ila );
    
  }
  
  
  @SuppressWarnings("unchecked")
  public void showCategories() {
	  Cursor cursor = mDb.fetchAllCategories();
	  
	  vectorCategories.add("All");
	  if (cursor.moveToFirst()) {
		  int titleIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CATEGORY_TITLE);
		  do {
			  vectorCategories.add( cursor.getString(titleIndex).toLowerCase());
		  }while( cursor.moveToNext() );
	  }
	  cursor.close();
	  spinnerArrayAdapter = new ArrayAdapter(this,
			  android.R.layout.simple_spinner_item, vectorCategories );
		    
	  spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	  spinner.setAdapter(spinnerArrayAdapter);
	  
	  spinner.setOnItemSelectedListener(spinnerListener);
	  
  }
  
  //spinner listener
  Spinner.OnItemSelectedListener spinnerListener =
   new Spinner.OnItemSelectedListener() {
    
   @SuppressWarnings("unchecked")
    public void onItemSelected(AdapterView parent, View v, int position, long id) {
      showDialog(DIALOG_LOADING_INCIDENTS);
      //showIncidents("deaths");
      showIncidents(vectorCategories.get(position));
      dismissDialog(DIALOG_LOADING_INCIDENTS);
   }
 
   @SuppressWarnings("unchecked")
    public void onNothingSelected(AdapterView parent) { }
 
  
  };
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch( requestCode ) {
      case LIST_INCIDENTS:
        if( resultCode != RESULT_OK ){
          break;
        }
        mHandler.post(mDisplayIncidents);
        //mark all incidents as read
        mDb.markAllIncidentssRead();  
        break;
        }
  }
  
}