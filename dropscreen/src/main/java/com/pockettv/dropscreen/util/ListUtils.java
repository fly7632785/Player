package com.pockettv.dropscreen.util;

import java.util.Collection;

/**
 * 说明：
 */
public class ListUtils {

    public static boolean isEmpty(Collection list){
        return !(list != null && list.size() != 0);
    }

}
