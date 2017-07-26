package com.pslcl.component.dummy;

public final class UnderTest {

    private static final int BreakpointNopVar = 30;

    private UnderTest() {
    }

    public static void main(String... args) {
        @SuppressWarnings("unused") // I need a breakpoint code line
        int x = BreakpointNopVar;
        int y = 30;

        String USER_HOME = System.getProperty("user.home");
        System.out.println("Java's report of System property user.home: " + USER_HOME);
    }

}