package org.example.knowqa.common;


import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class DefaultUser {
    public static final String ID="1";
    public static final String NAME="默认用户";
    public static String userIdOrDefault(String userId){
        return isBlank(userId)? ID:userId;
    }
    public static String userNameOrDefault(String userName){
        return isBlank(userName)? NAME:userName;
    }
    private static boolean isBlank(String v){
        return v==null|| v.isBlank();
    }
}
