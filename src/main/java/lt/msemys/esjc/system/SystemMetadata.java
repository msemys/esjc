package lt.msemys.esjc.system;

public class SystemMetadata {
    public static final String MAX_AGE = "$maxAge";
    public static final String MAX_COUNT = "$maxCount";
    public static final String TRUNCATE_BEFORE = "$tb";
    public static final String CACHE_CONTROL = "$cacheControl";
    public static final String ACL = "$acl";
    public static final String ACL_READ = "$r";
    public static final String ACL_WRITE = "$w";
    public static final String ACL_DELETE = "$d";
    public static final String ACL_META_READ = "$mr";
    public static final String ACL_META_WRITE = "$mw";
    public static final String USER_STREAM_ACL = "$userStreamAcl";
    public static final String SYSTEM_STREAM_ACL = "$systemStreamAcl";

    private SystemMetadata() {
    }
}
