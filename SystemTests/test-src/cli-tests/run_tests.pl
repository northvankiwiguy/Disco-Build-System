#!/usr/bin/perl
#
# This Perl script is the main entry point for running Disco's command-line-based
# regression tests. Use this script in one of two ways:
#
#  1) To run all regression test suites, use:
#       ./run_tests.pl disco-<version>.tar.gz
#  2) To run selected regression test suites, use:
#       ./run_tests.pl disco-<version>.tar.gz suite-<suite1> suite-<suite2> ...
#

use Cwd 'abs_path';
use strict;
use warnings;

# location for writing the test results
my $outputDir;

#--------------------------------------------------------------------------------
# fatalError
#
# Display an error message, then exit the whole program (never returning)
#--------------------------------------------------------------------------------

sub fatalError {
	my ($msg) = @_;
	
	print STDERR "Error: $msg\n";
	exit(1);
}

#--------------------------------------------------------------------------------
# testFailed
#
# Display an error message, to indicate that a test case failed.
#--------------------------------------------------------------------------------

sub testFailed {
	my ($msg) = @_;
	
	print "TEST FAILED: $msg\n";
}

#--------------------------------------------------------------------------------
# testPassed
#
# Display an OK message, to indicate that a test case succeeded
#--------------------------------------------------------------------------------

sub testPassed {
	print "OK\n";
}

#--------------------------------------------------------------------------------
# validateSuite
#
# Given a test suite name, ensure that the suite sub-directory exists and
# contains valid content.
#
# Returns true if the suite is valid, else false if there's an error.
#--------------------------------------------------------------------------------

sub validateSuite {
	my ($suite) = @_;

	return ((-e "$suite/README") && (-e "$suite/setup.sh") && (-e "$suite/teardown.sh"));
}

#--------------------------------------------------------------------------------
# validateTestCase
#
# Given a test case directory, ensure that the test sub-directory exists and
# contains valid content.
#
# Returns true if the test case is valid, else false if there's an error.
#--------------------------------------------------------------------------------

sub validateTestCase {
	my ($test) = @_;

	return ((-e "$test/run.sh") && (-e "$test/expected.txt"));
}

#--------------------------------------------------------------------------------
# executeSuite
#
# Given a test suite name, execute all the tests in that suite, displaying
# appropriate output to the console.
#
# Returns a pair of (number of tests executed, number of tests that passed).
#
#--------------------------------------------------------------------------------

sub executeSuite {
	my ($suite) = @_;
	
	my $testsExecuted = 0;
	my $testsPassed = 0;
	
	#
	# Execute all the test cases in this test suite, using this procedure:,
	#  1) Set up a test result directory (for temporary files and test output)
	#  2) Invoke setup.sh
	#  3) Invoke the test script itself, and capture the pass/fail result.
	#  4) Invoke teardown.sh (but only if the test passes).
	#
	my @testCases = glob("$suite/test-*");
	foreach my $test (@testCases) {
		
		print "$test - ";
		
		# Quickly validate the test case directory.
		if (!validateTestCase("$test")){
			testFailed("Test case $test does have a valid test directory.");			
		} 
		
		# Test case looks good, so execute it.
		else {	
			# Create a test-output directory.
			my $testOutputDir = "$outputDir/$test";
			if (system("mkdir -p $testOutputDir") != 0){
				fatalError("Failed to create directory: $testOutputDir");
			}
			
			#
			# We're going to change directory in order to execute scripts, 
			# but we still want to access files in the source directory, 
			# so we need the absolute path to our source directory.
			#
			my $testSrcDir = abs_path($test);
			$ENV{'TEST_SRC'} = $testSrcDir;
			
			# Run the setup.sh script (after cd'ing to the output directory)
			if (system("cd $testOutputDir && $testSrcDir/../setup.sh > setup.log 2>&1") != 0){
				testFailed("Problem while executing the setup.sh script");
			}
			
			# Setup was OK, so execute the script.
			else {
				if (system("cd $testOutputDir && $testSrcDir/run.sh > actual.log 2>&1") != 0){
					testFailed("Problem while executing the run.sh script");
				}
				
				# Diff the actual and expected output.
				else {
					if (system("cd $testOutputDir && diff -c actual.log $testSrcDir/expected.txt > diff.txt") != 0){
						testFailed("Actual and expected output don't match.\nSee $testOutputDir/diff.txt");
					} 
					
					# Success!
					else {
						$testsPassed++;
						testPassed();
						
						# Run the teardown script, to clean-up files.
						if (system("cd $testOutputDir && $testSrcDir/../teardown.sh > teardown.log 2>&1") != 0){
							# no error
						}
					}
				}
			}

		}
		$testsExecuted++;	
	}
	
	return ($testsExecuted, $testsPassed);
}

#--------------------------------------------------------------------------------
# Main entry point
#--------------------------------------------------------------------------------

#
# Validate command line arguments
#
if ($#ARGV < 0) {
	fatalError("Usage: run_tests.pl disco-<version>.tar.gz { suite-<name>, ...}");
}

#
# Locate where all our test output will be stored (outside of the source tree). We want
# to place it right next to the "test-src" directory, which is two levels up from this
# script.
#
if (-d "../cli-tests" && -d "../../test-src") {
	$outputDir = abs_path("../../test-output");
	if ((-e "$outputDir") && (system("chmod -R +w $outputDir") != 0)){
		fatalError("Unable to chmod old test output from $outputDir");
	}
	if (system("rm -rf $outputDir") != 0){
		fatalError("Unable to remove old test output from $outputDir");
	}		
} else {
	fatalError("This script must be run from within the cli-tests directory");
}

#
# Expand the release package and set the DISCO_HOME environment variable to point to it
#
my $packageName = $ARGV[0];
shift @ARGV;
my $expandDir = "$outputDir/discoHome";
if (! -e "$packageName") {
	fatalError("Release package $packageName doesn't exist.");
}
if (system("mkdir -p $expandDir && tar -C $expandDir -zxf $packageName") != 0){
	fatalError("Failed to extract Disco release package.");
}
my @subDir = glob("$expandDir/disco-*");
$ENV{"DISCO_HOME"} = $subDir[0];
$ENV{'PATH'} = $ENV{"DISCO_HOME"} . "/bin:" . $ENV{'PATH'};

#
# Look for the list of test suites to execute. If none are specified, select them all.
# Quickly validate them to ensure their sub-directories look sane (do this before we
# start to execute anything).
#
my @selectedSuites = @ARGV;
if ($#selectedSuites == -1) {
	@selectedSuites = glob("suite-*");
}
foreach my $suite (@selectedSuites) {
	if (!validateSuite($suite)) {
		fatalError("Suite $suite is not a valid test suite (bad name, or invalid suite directory)");
	}
}

#
# Execute the selected test suites, keeping track of the test failure count.
#
my $failedTests = 0;
my $failedSuites = 0;
my $totalTests = 0;
my $totalSuites = 0;

foreach my $suite (@selectedSuites) {
	my ($executedTests, $passedTests) = executeSuite($suite);
	
	if ($passedTests != $executedTests) {
		$failedSuites++;
		$failedTests += ($executedTests - $passedTests);
	}
	$totalTests += $executedTests;
	$totalSuites++;
}

#
# Display a final summary report
#
print "=======================================\n";
print "TESTING COMPLETED\n";
print "Total Tests Executed: $totalTests in $totalSuites suite(s)\n";
print "Tests Failed:         $failedTests in $failedSuites suite(s)\n";
print "=======================================\n";

#
# For error checking purposes, return a suitable exit code.
#
if ($failedTests != 0){
	exit(1);
} else {
	exit(0);
}