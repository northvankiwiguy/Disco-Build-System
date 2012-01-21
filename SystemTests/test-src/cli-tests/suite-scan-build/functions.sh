#
# Common functions used by more than one test case.
#

#
# function for executing a "command" and checking whether the
# expected "key" can be found in the output.
#
match()
{
	command=$1
	key=$2
	set +e
	$command | grep -sq -- "$key"
	success=$?
	set -e
	if [ $success -eq 0 ];
	then
		echo "$command matched output $key"
	else
		echo "$command DID NOT MATCH output $key" 
	fi
}

#
# function for executing a "command" and checking whether that
# the "key" can not be found in the output.
#
not_match()
{
	command=$1
	key=$2
	set +e
	$command | grep -sq -- "$key"
	success=$?
	set -e
	if [ $success -eq 0 ];
	then
		echo "$command INCORRECTLY MATCHED output $key"
	else
		echo "$command did not match incorrect output $key, as expected."
	fi
}

#
# function for ensuring that the command provides no output
#
match_empty()
{
	command=$1
	if [ `$command | wc -c` = "0" ];
	then
		echo "$command provides empty output, as expected"
	else
		echo "$command DOES NOT PROVIDE EMPTY OUTPUT"
	fi
}
