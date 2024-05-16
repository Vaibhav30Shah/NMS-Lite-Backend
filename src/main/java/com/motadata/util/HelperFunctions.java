package com.motadata.util;

public class HelperFunctions
{
    //check for aliveness of snmp object
    public static boolean isPingSuccessful(String ip) throws Exception
    {
        Process process = new ProcessBuilder("fping", ip).start();

        int exitCode = process.waitFor();

        return exitCode == 0;
    }
}
