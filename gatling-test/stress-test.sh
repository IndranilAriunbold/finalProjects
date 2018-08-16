#!/bin/bash

BASEDIR=$(cd "$(dirname "$0")"; pwd)
cd $BASEDIR

. ./stress-test-config.sh

MVN=${MVN:-mvn}
PROVISIONING_PARAMETERS=${PROVISIONING_PARAMETERS:-}
PROVISION_COMMAND="$MVN verify -P provision,import-dump $PROVISIONING_PARAMETERS -Ddataset=$dataset"
TEARDOWN_COMMAND="$MVN verify -P teardown"

function runCommand {
    echo "  $1"
    echo 
    if ! $debug; then eval "$1"; fi
}

function runTest {


    TEST_COMMAND="$MVN gatling:execute -Dgatling.simulationClass=simulations.login -DfilterResults=true  -DusersPerSec=$usersPerSec -DrampUpPeriod=15 -DwarmUpPeriod=15 -DmeasurementPeriod=30 -DuserThinkTime=5"

    echo "ITERATION: $(( i+1 )) / $maxIterations      $ITERATION_INFO"
    echo 


        runCommand "$TEST_COMMAND"
        export testResult=$?


    [[ $testResult != 0 ]] && echo "Test exit code: $testResult"

}



echo "Starting ${algorithm} stress test"
echo

usersPerSecTop=0

case "${algorithm}" in

    incremental)

        for (( i=0; i < $maxIterations; i++)); do

            usersPerSec=`echo "$usersPerSec0 + $i * $incrementFactor" | bc`

            runTest $@

            if [[ $testResult == 0 ]]; then 
                usersPerSecTop=$usersPerSec
            else
                echo "INFO: Last iteration failed. Stopping the loop."
                break
            fi

        done

    ;;

    *) 
        echo "Algorithm '${algorithm}' not supported."
        exit 1
    ;;

esac

echo "Highest load with passing test: $usersPerSecTop users per second"
