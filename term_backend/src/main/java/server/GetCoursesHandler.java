package server;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

public class GetCoursesHandler implements Route{

  @Override
  public Object handle(Request request, Response response) {
    List<List<String>> courseInformation = new ArrayList<List<String>>();

    try {
      Class.forName("org.sqlite.JDBC");
      String urlToDB = "jdbc:sqlite:" + "waitlist.sqlite3";
      Connection conn = DriverManager.getConnection(urlToDB);
      Statement stat = conn.createStatement();
      stat.executeUpdate("PRAGMA foreign_keys=ON;");
      PreparedStatement prep;
      ResultSet rs;

      prep = conn.prepareStatement("select * from classes");
      rs = prep.executeQuery();

      while(rs.next()){
        List<String> tempList = new ArrayList<String>();
        String title = rs.getString(2);
        String instructor = rs.getString(3);
        tempList.add(title);
        tempList.add(instructor);
        courseInformation.add(tempList);
        System.out.println("info: " + title + " " + instructor + " ");
      }

    } catch (SQLException e){
      System.out.println("caught this exception: " + e);
    } catch (ClassNotFoundException f){
      System.out.println("caught this exception: " + f);
    }
    return courseInformation;
  }


}
