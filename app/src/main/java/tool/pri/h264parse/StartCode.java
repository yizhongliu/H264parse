package tool.pri.h264parse;

public class StartCode {
    long index;
    int size;

    StartCode(long index, int size) {
        this.index = index;
        this.size = size;
    }

    long getNaluStartIndex() {
        return index;
    }

    int getNaluStartSize() {
        return size;
    }
}
