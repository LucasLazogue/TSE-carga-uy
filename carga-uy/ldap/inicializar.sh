# configuracion inicial del directorio, la misma del post_start del docker-compose.yml.
# se puede correr mas de una vez: lo que ya existe no se vuelve a crear
set -e
dir=$(dirname "$0")
until ldapsearch -x -H ldap://localhost:3389 -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" -b "" -s base > /dev/null 2>&1; do
  sleep 2
done
dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 backend suffix list | grep -q "^dc=carga,dc=uy " \
  || dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 backend create --suffix dc=carga,dc=uy --be-name carga --create-suffix
dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 pwpolicy set --pwdlockout on --pwdmaxfailures 5 --pwdlockoutduration 900
# el ldif puede venir con CRLF desde Windows
tr -d '\r' < "$dir/directorio.ldif" | ldapadd -c -x -H ldap://localhost:3389 -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" || [ $? -eq 68 ]
admin="uid=$LDAP_ADMIN_USUARIO,ou=usuarios,dc=carga,dc=uy"
if [ -n "$LDAP_ADMIN_USUARIO" ] && ! ldapsearch -x -H ldap://localhost:3389 -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" -b "$admin" -s base > /dev/null 2>&1; then
  ldapadd -x -H ldap://localhost:3389 -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" <<FIN
dn: $admin
objectClass: inetOrgPerson
uid: $LDAP_ADMIN_USUARIO
cn: $LDAP_ADMIN_USUARIO
sn: $LDAP_ADMIN_USUARIO
userPassword: $LDAP_ADMIN_PASSWORD

dn: cn=administrador,ou=grupos,dc=carga,dc=uy
changetype: modify
add: member
member: $admin
FIN
fi
# indice para paginar el listado de usuarios ordenado por uid sin recorrer todo el directorio
if ! dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 backend vlv-index list carga | grep -q "^cn: usuarios$"; then
  dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 backend vlv-index add-search --name usuarios --search-base ou=usuarios,dc=carga,dc=uy --search-scope 1 --search-filter "(objectClass=inetOrgPerson)" carga
  dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 backend vlv-index add-index --parent-name usuarios --index-name usuarios-uid --sort uid carga
  dsconf -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" ldap://localhost:3389 backend vlv-index reindex --parent-name usuarios carga
fi
ldapsearch -x -H ldap://localhost:3389 -D "cn=Directory Manager" -w "$DS_DM_PASSWORD" -b cn=administrador,ou=grupos,dc=carga,dc=uy member
