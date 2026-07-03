package com.saju.fortune.dto;

import com.saju.fortune.domain.Cheongan;
import com.saju.fortune.domain.Jiji;

public class SajuPalja {
    public Cheongan yearCheongan; 
    public Jiji yearJiji;   
    
    public Cheongan monthCheongan; 
    public Jiji monthJiji; 
    
    public Cheongan dayCheongan; 
    public Jiji dayJiji;     
    
    public Cheongan hourCheongan; 
    public Jiji hourJiji;   

    public String getPaljaString() {
        return String.format("%s%s %s%s %s%s %s%s",
            yearCheongan.getName(), yearJiji.getName(),
            monthCheongan.getName(), monthJiji.getName(),
            dayCheongan.getName(), dayJiji.getName(),
            hourCheongan.getName(), hourJiji.getName()
        );
    }
}