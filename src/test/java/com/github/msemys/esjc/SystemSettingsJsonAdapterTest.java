package com.github.msemys.esjc;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class SystemSettingsJsonAdapterTest {

    @Test
    public void serializesWithAllAcls() {
        StreamAcl userStreamAcl = new StreamAcl(asList("eric", "kyle", "stan", "kenny"),
            asList("butters"),
            asList("$admins"),
            asList("victoria", "mackey"),
            asList("randy"));

        StreamAcl systemStreamAcl = new StreamAcl(asList("$admins"),
            asList("$all"),
            asList("$admins"),
            null,
            asList("$all"));

        SystemSettings settings = new SystemSettings(userStreamAcl, systemStreamAcl);

        assertEquals("{\"$userStreamAcl\":{\"$r\":[\"eric\",\"kyle\",\"stan\",\"kenny\"]," +
            "\"$w\":\"butters\"," +
            "\"$d\":\"$admins\"," +
            "\"$mr\":[\"victoria\",\"mackey\"]," +
            "\"$mw\":\"randy\"}," +
            "\"$systemStreamAcl\":{\"$r\":\"$admins\"," +
            "\"$w\":\"$all\"," +
            "\"$d\":\"$admins\"," +
            "\"$mw\":\"$all\"}}", settings.toJson());
    }

    @Test
    public void serializesWithSystemStreamAclOnly() {
        StreamAcl systemStreamAcl = new StreamAcl(asList("$admins"), asList("$all"), null, null, asList("$all"));

        SystemSettings settings = new SystemSettings(null, systemStreamAcl);

        assertEquals("{\"$systemStreamAcl\":{\"$r\":\"$admins\"," +
            "\"$w\":\"$all\"," +
            "\"$mw\":\"$all\"}}", settings.toJson());
    }

    @Test
    public void serializesWithoutAcls() {
        SystemSettings settings = new SystemSettings(null, null);
        assertEquals("{}", settings.toJson());
    }

    @Test
    public void deserializesWithAllAcls() {
        SystemSettings settings = SystemSettings.fromJson("{\"$userStreamAcl\":{\"$r\":[\"eric\",\"kyle\",\"stan\",\"kenny\"]," +
            "\"$w\":\"butters\"," +
            "\"$d\":\"$admins\"," +
            "\"$mr\":[\"victoria\",\"mackey\"]," +
            "\"$mw\":\"randy\"}," +
            "\"$systemStreamAcl\":{\"$r\":\"$admins\"," +
            "\"$w\":\"$all\"," +
            "\"$d\":\"$admins\"," +
            "\"$mw\":\"$all\"}}");

        assertNotNull(settings.userStreamAcl);
        assertEquals(asList("eric", "kyle", "stan", "kenny"), settings.userStreamAcl.readRoles);
        assertEquals(asList("butters"), settings.userStreamAcl.writeRoles);
        assertEquals(asList("$admins"), settings.userStreamAcl.deleteRoles);
        assertEquals(asList("victoria", "mackey"), settings.userStreamAcl.metaReadRoles);
        assertEquals(asList("randy"), settings.userStreamAcl.metaWriteRoles);

        assertNotNull(settings.systemStreamAcl);
        assertEquals(asList("$admins"), settings.systemStreamAcl.readRoles);
        assertEquals(asList("$all"), settings.systemStreamAcl.writeRoles);
        assertEquals(asList("$admins"), settings.userStreamAcl.deleteRoles);
        assertNull(settings.systemStreamAcl.metaReadRoles);
        assertEquals(asList("$all"), settings.systemStreamAcl.metaWriteRoles);
    }

    @Test
    public void deserializesWithUserStreamAclOnly() {
        SystemSettings settings = SystemSettings.fromJson("{\"$userStreamAcl\":{\"$w\":\"butters\"," +
            "\"$d\":\"$admins\"," +
            "\"$mr\":[\"victoria\",\"mackey\"]," +
            "\"$mw\":\"randy\"}}");

        assertNotNull(settings.userStreamAcl);
        assertNull(settings.userStreamAcl.readRoles);
        assertEquals(asList("butters"), settings.userStreamAcl.writeRoles);
        assertEquals(asList("$admins"), settings.userStreamAcl.deleteRoles);
        assertEquals(asList("victoria", "mackey"), settings.userStreamAcl.metaReadRoles);
        assertEquals(asList("randy"), settings.userStreamAcl.metaWriteRoles);

        assertNull(settings.systemStreamAcl);
    }

}
