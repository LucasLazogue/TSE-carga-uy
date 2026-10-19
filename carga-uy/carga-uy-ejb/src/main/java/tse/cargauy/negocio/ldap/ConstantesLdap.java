package tse.cargauy.negocio.ldap;

final class ConstantesLdap {

    static final String BASE_DN = "dc=carga,dc=uy";
    static final String USERS_DN = "ou=usuarios," + BASE_DN;
    static final String GROUPS_DN = "ou=grupos," + BASE_DN;
    static final String DIRECTORY_MANAGER_DN = "cn=Directory Manager";

    static final String USER_ID = "uid";
    static final String COMMON_NAME = "cn";
    static final String SURNAME = "sn";
    static final String MEMBER = "member";
    static final String OBJECT_CLASS = "objectClass";
    static final String USER_PASSWORD = "userPassword";

    static final String INET_ORG_PERSON = "inetOrgPerson";
    static final String GROUP_OF_NAMES = "groupOfNames";

    private ConstantesLdap() {
    }
}
