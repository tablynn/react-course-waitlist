package server;

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

//Query Parameters: student name, class name
// http://localhost:3231/removeStudent?studentName=Christine%20Wu&className=CSCI%201470:%20Deep%20Learning

/**
 * This class removes a student from a waitlist and retreieves an updated waitlist
 * It is run when a fetch to the API server is made with the endpoint removeStudent given parameters
 * studentName email and className
 */
public class RemoveStudentHandler implements Route {

  @Override
  public Object handle(Request request, Response response) {
    QueryParamsMap qm = request.queryMap();

    // If the query parameters are valid, assign parameters to String values and run helper handleTables
    if (qm.hasKey("studentName") && qm.hasKey("className")){
      String studentName = qm.value("studentName");
      String className = qm.value("className");

      return handleTables(studentName, className);

    } else {
      return "failure with the provided query parameters";
    }
  }

  //This method checks if there is an enrollment matching the student and class criteria to remove
  //and then subseuently deletes the row from the enrollments database
  public List<String> handleTables(String studentName, String className){
    List<String> studentWaitlist = new ArrayList<String>();

    try {
      //Establish connection to database
      Class.forName("org.sqlite.JDBC");
      String urlToDB = "jdbc:sqlite:" + "waitlist.sqlite3";
      Connection conn = DriverManager.getConnection(urlToDB);
      Statement stat = conn.createStatement();
      //Tell database to enforce foreign keys
      stat.executeUpdate("PRAGMA foreign_keys=ON;");
      PreparedStatement prep = null;
      ResultSet rs = null;

      // Get class_id from 'classes' table
      Integer classID = this.getClassID(prep, conn, rs, className);
      System.out.println("the class " + className + " has classID " + classID);

      // Get student_id from 'students' table
      Integer studentID = this.getStudentID(prep, conn, rs, studentName);
      System.out.println("the student " + studentName + " has studentID " + studentID);

      // Check if classID-studentID pair exists in 'enrollments' table
      prep = conn.prepareStatement("select * from enrollments WHERE student_id = ?");
      prep.setInt(1, studentID);
      rs = prep.executeQuery();
      //boolean initially set to false
      Boolean studentExistsInEnrollmentsWithCorrectClass = false;
      Integer enrollID = 0;

      //If there are any rows matching the executed query for student ID, class ID is checked
      //boolean is then updated if class ID is a match
      while(rs.next()){
        Integer currClassID = rs.getInt(3);
        if(currClassID == classID){
          studentExistsInEnrollmentsWithCorrectClass = true;
          enrollID = rs.getInt(1);
        }
      }

      // If pair exists, remove pair from 'enrollments' table
      if(studentExistsInEnrollmentsWithCorrectClass){
        this.removeStudent(prep, conn, rs, enrollID);
        System.out.println("removed the pair: studentID= " + studentID + " classID= " + classID + " from enrollments table");
      }

      // Return: list of names of students in waitlist in order for that specific course
      studentWaitlist = this.createReturnedWaitlist(prep, conn, rs, classID);

    } catch (SQLException e){
      System.out.println("caught this exception: " + e);
    } catch (ClassNotFoundException f){
      System.out.println("caught this exception: " + f);
    }

    return studentWaitlist;
  }

  //helper method returning class ID based on className
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

  //helper method returning student ID based on studentName
  private Integer getStudentID(PreparedStatement prep, Connection conn, ResultSet rs, String studentName)
      throws SQLException {
    prep = conn.prepareStatement("select * from students WHERE name = ?");
    prep.setString(1, studentName);
    rs = prep.executeQuery();
    Integer studentID = 0;

    while(rs.next()){
      studentID = rs.getInt(1);
    }
    return studentID;
  }

  //helper method that deletes row from enrollments table based on unique enroll_id value
  private void removeStudent(PreparedStatement prep, Connection conn, ResultSet rs, Integer enrollID)
      throws SQLException {

    prep = conn.prepareStatement("DELETE FROM enrollments WHERE enroll_id = ?");
    prep.setInt(1, enrollID);
    prep.executeUpdate();
    System.out.println("removing the enrollID: " + enrollID);
  }

  //Generates nested list of Strings that represents waitlist of students for a given course
  // Enrollments table is queried for matching class ID of course and each matching row is added
  //to inner List representing a student. Then each List is added to an outer List representing the
  //waitlist. The nested List is returned.
  private List<String> createReturnedWaitlist(PreparedStatement prep, Connection conn, ResultSet rs, Integer classID)
      throws SQLException {
    List<String> studentWaitlist = new ArrayList<String>();
    List<String> studentIDList = new ArrayList<String>();

    prep = conn.prepareStatement("select * from enrollments WHERE class_id = ?");
    prep.setInt(1, classID);
    rs = prep.executeQuery();

    while(rs.next()){
      studentIDList.add(rs.getString(2));
    }
    System.out.println("created a list of relevant student IDs: " + studentIDList);

    prep = conn.prepareStatement("select * from students WHERE student_id = ?");
    for (int i = 0; i < studentIDList.size(); i++){
      prep.setString(1, studentIDList.get(i));
      rs = prep.executeQuery();

      while(rs.next()){
        studentWaitlist.add(rs.getString(2));
      }
    }
    System.out.println("created list of student names to be returned: " + studentWaitlist);
    return studentWaitlist;
  }
}
