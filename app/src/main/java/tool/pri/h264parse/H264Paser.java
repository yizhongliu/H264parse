package tool.pri.h264parse;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class H264Paser {
    private final static String TAG = "H264Paser";

    public final static int READ_ERROR = -1;
    public final static int READ_END = -2;
    public final static int FORMAT_ERROR = -3;

    public final static int NALU_TYPE_SLICE    = 1;
    public final static int NALU_TYPE_DPA      = 2;
    public final static int NALU_TYPE_DPB      = 3;
    public final static int NALU_TYPE_DPC      = 4;
    public final static int NALU_TYPE_IDR      = 5;
    public final static int NALU_TYPE_SEI      = 6;
    public final static int NALU_TYPE_SPS      = 7;
    public final static int NALU_TYPE_PPS      = 8;
    public final static int NALU_TYPE_AUD      = 9;
    public final static int NALU_TYPE_EOSEQ    = 10;
    public final static int NALU_TYPE_EOSTREAM = 11;
    public final static int NALU_TYPE_FILL     = 12;

    public final static int NALU_PRIORITY_DISPOSABLE = 0;
    public final static int NALU_PRIRITY_LOW         = 1;
    public final static int NALU_PRIORITY_HIGH       = 2;
    public final static int NALU_PRIORITY_HIGHEST    = 3;


    int parseH264(String fileName) {
        File file = new File(fileName);
        RandomAccessFile in = null;

        try {
            in = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<StartCode> startIndexs = parseStartIndex(in);
        Log.e(TAG, "startIndexs size:" + startIndexs.size());

        for (int i = 0; i < startIndexs.size(); i++) {

            Nalu nalu = new Nalu();

            try {
                in.seek(startIndexs.get(i).getNaluStartIndex());

                byte naluHeader = in.readByte();

                //forbidden_bit 禁止位：编码中默认值为 0，当网络识别此单元中存在比特错误时，可将其设为 1，以便接收方丢掉该单元，主要用于适应不同种类的网络环境
                nalu.setForbiddenBit(naluHeader & 0x80);
                //nal_reference_bit 重要性指示位：用于在重构过程中标记一个 NAL 单元的重要性，值越大
                nalu.setNalReferenceIdc(naluHeader & 0x60);
                //nal_unit_type：NALU 类型位: 可以表示 NALU 的 32 种不同类型特征
                nalu.setNalUnitType(naluHeader & 0x1f);

                if (i == startIndexs.size() - 1) {
                    nalu.setDataLength((int) (in.length() - startIndexs.get(i).getNaluStartIndex()));
                } else {
                    nalu.setDataLength((int) (startIndexs.get(i+1).index - startIndexs.get(i).getNaluStartIndex()));
                }

                String naluType;
                switch (nalu.getNalUnitType()) {
                    case NALU_TYPE_SLICE:
                        naluType = "SLICE";
                        break;
                    case NALU_TYPE_DPA:
                        naluType = "DPA";
                        break;
                    case NALU_TYPE_DPB:
                        naluType = "DPB";
                        break;
                    case NALU_TYPE_DPC:
                        naluType = "DPC";
                        break;
                    case NALU_TYPE_IDR:
                        naluType = "IDR";
                        break;
                    case NALU_TYPE_SEI:
                        naluType = "SEI";
                        break;
                    case NALU_TYPE_SPS:
                        naluType = "SPS";
                        break;
                    case NALU_TYPE_PPS:
                        naluType = "PPS";
                        break;
                    case NALU_TYPE_AUD:
                        naluType = "AUD";
                        break;
                    case NALU_TYPE_EOSEQ:
                        naluType = "EOSEQ";
                        break;
                    case NALU_TYPE_EOSTREAM:
                        naluType = "EOSTREAM";
                        break;
                    case NALU_TYPE_FILL:
                        naluType = "FILL";
                        break;
                    default:
                        naluType = "None";
                        break;
                }

                String referenceType;
                switch (nalu.getNalReferenceIdc() >> 5) {
                    case NALU_PRIORITY_DISPOSABLE:
                        referenceType = "DISPOS";
                        break;
                    case NALU_PRIRITY_LOW:
                        referenceType = "LOW";
                        break;
                    case NALU_PRIORITY_HIGH:
                        referenceType = "HIGH";
                        break;
                    case NALU_PRIORITY_HIGHEST:
                        referenceType = "HIGHEST";
                        break;
                    default:
                        referenceType = "None";

                }

                Log.e(TAG, " NUM |    POS  |    IDC |  TYPE |   LEN   |");
                Log.e(TAG, " " + i + " | " + startIndexs.get(i).getNaluStartIndex() + " | " + referenceType + " | " + naluType + " | " + nalu.getDataLength());


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }


    List<StartCode> parseStartIndex(RandomAccessFile in) {
        List<StartCode> startIndexs = new ArrayList<>();

        int startSize = 0;

        try {
            byte startIndex3 = 0;
            byte[] startIndexBuff = new byte[3];

            if (in.read(startIndexBuff) != 3) {
                Log.e(TAG, "read count:" + in.read(startIndexBuff));
                return startIndexs;
            }

            while (in.getFilePointer() != in.length()) {

                if (startIndexBuff[0] != 0x00 || startIndexBuff[1] != 0x00 || startIndexBuff[2] != 0x01) {
                    startIndex3 = in.readByte();
                    if (startIndexBuff[0] != 0x00 || startIndexBuff[1] != 0x00 || startIndexBuff[2] != 0x00 || startIndex3 != 0x01) {
                        startSize = -1;
                    } else {
                        startSize = 4;
                    }
                } else {
                    startSize = 3;
                }

                if (startSize != -1) {
                    startIndexs.add(new StartCode(in.getFilePointer() - startSize, startSize));
                    if (in.read(startIndexBuff) != 3) {
                        return startIndexs;
                    }
                } else {
                    startIndexBuff[0] = startIndexBuff[1];
                    startIndexBuff[1] = startIndexBuff[2];
                    startIndexBuff[2] = startIndex3;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return startIndexs;
    }
}
