#
#
#

if [ -z "$*" ]; then
	echo dropdb rallymaster
	dropdb rallymaster

	echo createdb -O rallymaster rallymaster
	createdb -O rallymaster rallymaster

	echo flyway migrate
	flyway migrate

	echo
	echo "Now restart the server and run again with any arguments to create users."
else
	echo curl -s -X POST "http://localhost:8080/api/auth/register?email=admin&password=athena"
	curl -s -X POST "http://localhost:8080/api/auth/register?email=admin&password=athena"
	echo

	echo curl -s -X POST "http://localhost:8080/api/auth/register?email=jpl@showpage.org&password=athena"
	curl -s -X POST "http://localhost:8080/api/auth/register?email=jpl@showpage.org&password=athena"
	echo

	echo curl -s -X POST "http://localhost:8080/api/auth/register?email=joe@showpage.org&password=athena"
	curl -s -X POST "http://localhost:8080/api/auth/register?email=joe@showpage.org&password=athena"
	echo
fi
