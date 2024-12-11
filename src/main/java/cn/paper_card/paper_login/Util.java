package cn.paper_card.paper_login;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;

class Util {

    static @NotNull SimpleDateFormat dateFormat() {
        return new SimpleDateFormat("yyyy年MM月dd日_HH:mm:ss");
    }
}
