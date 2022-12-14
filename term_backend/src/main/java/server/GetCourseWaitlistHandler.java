package server;

import com.squareup.moshi.Moshi;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;

// http://localhost:3231/getCourseWaitlist?className=CSCI%201470:%20Deep%20Learning

/**
 * This class retrieves the waitlist data for a single class and is run when a fetch to the API
 * server is made with the endpoint getCourseWaitlist given request parameter className
 */
public class GetCourseWaitlistHandler implements Route {

  @Override
  public Object handle(Request request, Response response) {
    List<List<String>> courseInformation = new ArrayList<List<String>>();
    QueryParamsMap qm = request.queryMap();

    // If the query parameter is valid, assign parameter to String values and run helper handleTables
    if (qm.hasKey("className")){
      String className = qm.value("className");
      return handleTables(className);
    } else {
      return "failure with the provided query parameters";
    }
  }

  public Object handleTables(String className){
    List<List<String>> courseInformation = new ArrayList<List<String>>();

    try {
      //Establish connection to database
      Class.forName("org.sqlite.JDBC");
      String urlToDB = "jdbc:sqlite:" + "waitlist.sqlite3";
      Connection conn = DriverManager.getConnection(urlToDB);
      Statement stat = conn.createStatement();
      //Tell the database to enforce foreign keys
      stat.executeUpdate("PRAGMA foreign_keys=ON;");
      PreparedStatement prep = null;
      ResultSet rs = null;

      // Get class_id from "classes" table
      Integer classID = this.getClassID(prep, conn, rs, className);
      System.out.println("the class " + className + " has classID " + classID);

      // use classID to get all the students on that waitlist by
      // querying enrollments table based on class ID corresponding to request parameter className
      List<String> studentIDList = new ArrayList<String>();

      prep = conn.prepareStatement("select * from enrollments WHERE class_id = ?");
      prep.setInt(1, classID);
      rs = prep.executeQuery();

      // create list of student IDs matching class ID queried on
      while(rs.next()){
        studentIDList.add(rs.getString(2));
      }
      System.out.println("created a list of relevant student IDs: " + studentIDList);

      // use student ID list to get all student names/emails from students table query
      prep = conn.prepareStatement("select * from students WHERE student_id = ?");
      for (int i = 0; i < studentIDList.size(); i++){
        prep.setString(1, studentIDList.get(i));
        rs = prep.executeQuery();

        //While there is still a next row, write each of the values of each attribute in the row to
        //a String, and add each String to an inner list. Then, add the list corresponding to the
        //data for a single student to an outer list representing the waitlist of students
        while(rs.next()){
          List<String> innerList = new ArrayList<String>();
          String studentName = rs.getString(2);
          String email = rs.getString(3);
          innerList.add(studentName);
          innerList.add(email);
          courseInformation.add(innerList);
        }
      }

    } catch (SQLException e){
      System.out.println("caught this exception: " + e);
    } catch (ClassNotFoundException f){
      System.out.println("caught this exception: " + f);
    }

    System.out.println("created list of student names to be returned: " + courseInformation);

    // Serializes responses into JSON format
    Moshi moshi = new Moshi.Builder().build();
    return moshi.adapter(List.class).toJson(courseInformation);
  }

  //helper method that queries classes table and returns class ID based on a className
  private Integer getClassID(PreparedStatement prep, Connection conn, ResultSet rs, String className)
      throws SQLException {
    prep = conn.prepareStatement("select * from classes WHERE title = ?");
    prep.setString(1, className);
    rs = prep.executeQuery();
    Integer classID = 0;

    while (rs.next()) {
      classID = rs.getInt(1);
    }
    return classID;
  }
}
