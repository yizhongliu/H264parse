package tool.pri.h264parse;

public class Nalu {
    int forbiddenBit;            //! should be always FALSE
    int nalReferenceIdc;        //! NALU_PRIORITY_xxxx
    int nalUnitType;            //! NALU_TYPE_xxxx

    int dataLength;
    byte[] rbsp;

    public int getForbiddenBit() {
        return forbiddenBit;
    }

    public void setForbiddenBit(int forbiddenBit) {
        this.forbiddenBit = forbiddenBit;
    }

    public int getNalReferenceIdc() {
        return nalReferenceIdc;
    }

    public void setNalReferenceIdc(int nalReferenceIdc) {
        this.nalReferenceIdc = nalReferenceIdc;
    }

    public int getNalUnitType() {
        return nalUnitType;
    }

    public void setNalUnitType(int nalUnitType) {
        this.nalUnitType = nalUnitType;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }
}
