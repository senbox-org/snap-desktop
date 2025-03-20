package org.esa.snap.rcp.actions.window;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenHSVImageViewActionTest {

    @Test
    @STTM("SNAP-3968")
    public void testConvertHSVToRGBExpressions() {

        final String[] hsvExpressions = new String[]{"0.3", "0.5", "0.7"};
        final String[] rgbaExpressions = OpenHSVImageViewAction.convertHSVToRGBExpressions(hsvExpressions);

        final String rExp = "min(round( (floor((6*(0.3))%6)==0?(0.7): (floor((6*(0.3))%6)==1?((1-((0.5)*(((6*(0.3))%6)-floor((6*(0.3))%6))))*(0.7)): (floor((6*(0.3))%6)==2?((1-(0.5))*(0.7)): (floor((6*(0.3))%6)==3?((1-(0.5))*(0.7)): (floor((6*(0.3))%6)==4?((1-((0.5)*(1-((6*(0.3))%6)+floor((6*(0.3))%6))))*(0.7)): (floor((6*(0.3))%6)==5?(0.7):0)))))) *256), 255)";
        final String gExp = "min(round( (floor((6*(0.3))%6)==0?((1-((0.5)*(1-((6*(0.3))%6)+floor((6*(0.3))%6))))*(0.7)): (floor((6*(0.3))%6)==1?(0.7): (floor((6*(0.3))%6)==2?(0.7): (floor((6*(0.3))%6)==3?((1-((0.5)*(((6*(0.3))%6)-floor((6*(0.3))%6))))*(0.7)): (floor((6*(0.3))%6)==4?((1-(0.5))*(0.7)): (floor((6*(0.3))%6)==5?((1-(0.5))*(0.7)):0)))))) *256), 255)";
        final String bExp = "min(round( (floor((6*(0.3))%6)==0?((1-(0.5))*(0.7)): (floor((6*(0.3))%6)==1?((1-(0.5))*(0.7)): (floor((6*(0.3))%6)==2?((1-((0.5)*(1-((6*(0.3))%6)+floor((6*(0.3))%6))))*(0.7)): (floor((6*(0.3))%6)==3?(0.7): (floor((6*(0.3))%6)==4?(0.7): (floor((6*(0.3))%6)==5?((1-((0.5)*(((6*(0.3))%6)-floor((6*(0.3))%6))))*(0.7)):0)))))) *256), 255)";

        assertEquals(rExp, rgbaExpressions[0]);
        assertEquals(gExp, rgbaExpressions[1]);
        assertEquals(bExp, rgbaExpressions[2]);
    }
}
