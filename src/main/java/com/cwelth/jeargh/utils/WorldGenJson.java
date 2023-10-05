package com.cwelth.jeargh.utils;

/*
	{
		"block": "",
		"distrib": "",
		"dim": "",
		"dropsList": [
			{
				"itemStack": ""
			}
		]
	}
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldGenJson {
    String block = "";
    String distrib = "";
    String distrib_raw = "";
    String dim = "";
    public class WGItemDrop {
        String itemStack = "";
        Map<String, Float> fortunes = new HashMap<>();
        public WGItemDrop(String itemStack, float dropAmount)
        {
            this.itemStack = itemStack;
            fortunes.put("0", dropAmount);
        }
    }
    List<WGItemDrop> dropsList = new ArrayList<>();
    public WorldGenJson(String block, String drop, String distrib, String distrib_raw, String dim)
    {
        this.block = block;
        this.distrib = distrib;
        this.dim = dim;
        this.distrib_raw = distrib_raw;
        String[] dropItem = drop.split(";");
        for(String singleItem : dropItem)
        {
            String[] dropString = singleItem.split(",");
            this.dropsList.add(new WGItemDrop(dropString[0], Float.parseFloat(dropString[1])));
        }
    }

    public static String BuildDistrib(HashMap<Integer, Integer> distrib, int profiled_blocks_y_level)
    {
        /*
            profiled_blocks - 100%
            distrib - x%
        */
        String toRet = "";
        for(Map.Entry<Integer, Integer> height: distrib.entrySet())
        {
            float chance = (float)height.getValue() / (float)profiled_blocks_y_level;
            String toAdd = height.getKey() + "," + chance;
            toRet += (toRet.isEmpty())? toAdd : ";" + toAdd;
        }
        return toRet;
    }

    public static String BuildRawDistrib(HashMap<Integer, Integer> distrib)
    {
        String toRet = "";
        for(Map.Entry<Integer, Integer> height: distrib.entrySet())
        {
            String toAdd = height.getKey() + "," + height.getValue();
            toRet += (toRet.isEmpty())? toAdd : ";" + toAdd;
        }
        return toRet;
    }

}
