package org.bob.learn.common.constant;

public enum WebServerType {

    TOMCAT,UNDERTOW,UNSUPPORTED;

    public static WebServerType serverType(){
        if(isTomcat()) {
            return TOMCAT;
        }
        if(isUndertow()){
            return UNDERTOW;
        }
        return UNDERTOW;
    }

    private static boolean isTomcat() {
        try{
            Class.forName("org.apache.catalina.startup.Tomcat");
            return true;
        }catch(Exception e){
           return false;
        }
    }

    private static boolean isUndertow() {
        try{
            Class.forName("io.undertow.Undertow");
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
