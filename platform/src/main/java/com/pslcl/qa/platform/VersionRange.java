package com.pslcl.qa.platform;

/**
 * This class represents a version range in the format of one of the following:
 *   A, A.B, A.B.C
 *   [A-B], (A-B], (A-B), [A-B)
 */
public class VersionRange {
    boolean min_open = false, max_open;
    int[] numbers = new int[] { 0, 0, 0, 0, 0, 0 };

    public VersionRange(String range) throws Exception {
        try {
            range = range.trim();
            range = range.replace( " ", "" );

            int stage = 0;
            for ( int i = 0; i < range.length(); i++ ) {
                if ( stage > 5 )
                    throw new Exception();

                Character c = range.charAt( i );
                if ( Character.isDigit( c ) ) {
                    numbers[stage] = numbers[stage]*10 + (c - '0');
                }
                else if ( c == '(' ) {
                    if ( i > 0 )
                        throw new Exception();
                    min_open = true;
                }
                else if ( c == '[' ) {
                    if ( i > 0 )
                        throw new Exception();
                    min_open = false;
                }
                else if ( c == '.' ) {
                    if ( stage == 2 )
                        throw new Exception();

                    stage += 1;
                }
                else if ( c == '-' ) {
                    if ( stage >= 3 )
                        throw new Exception();
                    stage = 3;
                }
                else if ( c == ']' ) {
                    if ( stage < 3 )
                        throw new Exception();
                    max_open = false;
                    stage = 10;
                }
                else if ( c == ')' ) {
                    if ( stage < 3 )
                        throw new Exception();
                    max_open = true;
                    stage = 10;
                }
                else
                    throw new Exception();
            }

            /* If only a low is given, create the high. */
            if ( stage < 3 ) {
                // A.B.C-D.E.F
                // 0.1.2-3.4.5
                for ( int i = 0; i < stage+1; i++ )
                    numbers[3+i] = numbers[i];
                numbers[3+stage] += 1;
                max_open = true;
            }
        }
        catch ( Exception e ) {
            throw new Exception( "Error: Illegal version range " + range );
        }
    }

    public boolean contains( String v ) {
        String[] fields = v.split( "\\." );
        int[] num = new int[3];

        for ( int i = 0; i < num.length && i < fields.length; i++ )
            num[i] = Integer.parseInt( fields[i] );

        if ( num[0] > numbers[3] )
            return false;
        if ( num[0] < numbers[0] )
            return false;

        boolean major_min = num[0] == numbers[0];
        boolean major_max = num[0] == numbers[3];

        if ( major_max && num[1] > numbers[4] )
            return false;
        if ( major_min && num[1] < numbers[1] )
            return false;

        boolean minor_min = major_min && num[1] == numbers[1];
        boolean minor_max = major_max && num[1] == numbers[4];

        if ( major_max && minor_max && num[2] > numbers[5] )
            return false;
        if ( major_min && minor_min && num[2] < numbers[2] )
            return false;

        boolean patch_min = major_min && minor_min && num[2] == numbers[2];
        boolean patch_max = major_max && minor_max && num[2] == numbers[5];

        if ( major_min && minor_min && patch_min )
            return ! min_open;
        if ( major_max && minor_max && patch_max )
            return ! max_open;

        return true;
    }
}
