package biz.redsoft;

/**
 * Created by Vasiliy on 14.07.2017.
 */
public interface IFBUser {

  void setUserName(String userName);

  String getUserName();

  void setPassword(String password);

  String getPassword();

  void setFirstName(String firstName);

  String getFirstName();

  void setMiddleName(String middleName);

  String getMiddleName();

  void setLastName(String lastName);

  String getLastName();

  void setGroupId(int groupId);

  int getGroupId();

  void setUserId(int userId);

  int getUserId();
}
