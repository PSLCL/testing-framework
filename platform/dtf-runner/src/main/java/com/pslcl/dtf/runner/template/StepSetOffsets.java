package com.pslcl.dtf.runner.template;

import java.util.List;

public class StepSetOffsets {
    private int beginSetOffset = -1;
    private int finalSetOffset = -1; // always non-negative when begin... becomes non-negative; never less than begin...

    StepSetOffsets(String stepString, List<String> setSteps, int initialSetStepCount) {
        int tempFinalSetOffset = initialSetStepCount;
        int setOffset = initialSetStepCount;
        while (true) {
            SetStep setStep = new SetStep(setSteps.get(setOffset));
            if (!setStep.getCommand().equals(stepString))
                break;
            this.beginSetOffset = initialSetStepCount;
            this.finalSetOffset = tempFinalSetOffset;
            if (++tempFinalSetOffset >= setSteps.size())
                break;
            setOffset = tempFinalSetOffset; // there is another step in this set
        }
    }

    int getBeginSetOffset() {
        return this.beginSetOffset;
    }

    int getFinalSetOffset() {
        return this.finalSetOffset;
    }

}