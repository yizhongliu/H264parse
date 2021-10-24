package tool.pri.h264parse;

import android.util.Log;

import androidx.annotation.Nullable;

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
    private final static Boolean DEBUG = true;

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

    public int width;
    public int height;

    public OnPaserCallback callback;

    public void startPaserH264(String filePath) {
        PaserThread paserThread = new PaserThread(filePath);
        paserThread.start();
    }


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
                in.seek(startIndexs.get(i).getNaluStartIndex() + startIndexs.get(i).getNaluStartSize());

                byte naluHeader = in.readByte();

                //forbidden_bit 禁止位：编码中默认值为 0，当网络识别此单元中存在比特错误时，可将其设为 1，以便接收方丢掉该单元，主要用于适应不同种类的网络环境
                nalu.setForbiddenBit(naluHeader & 0x80);
                //nal_reference_bit 重要性指示位：用于在重构过程中标记一个 NAL 单元的重要性，值越大
                nalu.setNalReferenceIdc(naluHeader & 0x60);
                //nal_unit_type：NALU 类型位: 可以表示 NALU 的 32 种不同类型特征
                nalu.setNalUnitType(naluHeader & 0x1f);

                if (i == startIndexs.size() - 1) {
                    nalu.setDataLength((int) (in.length() - startIndexs.get(i).getNaluStartIndex() - startIndexs.get(i).getNaluStartSize()));
                } else {
                    nalu.setDataLength((int) (startIndexs.get(i+1).index - startIndexs.get(i).getNaluStartIndex() - startIndexs.get(i).getNaluStartSize()));
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
                        byte[] spsData = new byte[nalu.getDataLength() - 1];
                        in.read(spsData);
                        paserSPS(spsData);
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

        if (callback != null) {
            callback.onPaserDone();
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



    public class PaserThread extends Thread {
        String path;

        PaserThread(String videoPath) {
            path = videoPath;
        }


        public void run() {
            parseH264(path);
        }
    }

    public void setPaserListener(OnPaserCallback callback) {
        this.callback = callback;
    }

    public interface OnPaserCallback {
        public void onPaserDone();
    }


    /*
    *   u(n)表示：使用n位无符号整数表示，由n位bit换算得到，即从左到右读取n个bit位所表示的值
    *   1 byte = 8 bit
    *   sps 是用 bit为单位来组织数据
    *   @param data  sps 数据
    *   @param size  数据占用的bit数量
    * */
    public int u(byte[] data, int size) {
        int val = 0;
        for (int i = 0; i < size; i++) {
            val <<= 1;
            if ((data[spsIndex / 8] & (0x80 >> (spsIndex % 8))) != 0) {
                val += 1;
            }
            spsIndex++;
        }

        return val;
    }

    /*
     * 无符号指数哥伦布编码(UE)
     * 哥伦布编码的码字code_word由三部分组成：code_word = [M个0] + [1] + [Info]
     * 根据码字code_word解码出code_num值的过程如下：
     *  1. 首先读入M位以"1"为结尾的0；
     *  2. 根据得到的M，读入接下来的M位Info数据；
     *  3. 根据这个公式得到计算结果code_num = Info – 1 + 2M
     */
    public int ue(byte[] data) {
        int zeroNum = 0;
        while (u(data, 1) == 0 && zeroNum < 32) {
            zeroNum ++;
        }

        return ((1 << zeroNum) - 1 + u(data, zeroNum));
    }

    int se(byte[] data)
    {
        int ueVal = ue(data);
        double k = ueVal;

        int seVal = (int)Math.ceil(k / 2);     //ceil:返回大于或者等于指定表达式的最小整数
        if (ueVal % 2 == 0) {       //偶数取反，即(-1)^(k+1)
            seVal = -seVal;
        }

        return seVal;
    }

    int spsIndex = 0;
    public void paserSPS(byte[] spsData) {
        spsIndex = 0;

        int profile_idc = u(spsData, 8);

        u(spsData, 1);      //constraint_set0_flag
        u(spsData, 1);      //constraint_set1_flag
        u(spsData, 1);      //constraint_set2_flag
        u(spsData, 1);      //constraint_set3_flag
        u(spsData, 1);      //constraint_set4_flag
        u(spsData, 1);      //constraint_set4_flag
        u(spsData, 2);      //reserved_zero_2bits
        int level_idc = u(spsData, 8);

        if (DEBUG)
            Log.e(TAG, "profile_idc :" + profile_idc + ",  level_idc:" + level_idc);

        int seq_parameter_set_id = ue(spsData);    //seq_parameter_set_id

        int chroma_format_idc = 1;     //摄像机出图大部分格式是4:2:0
        if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 ||
                profile_idc == 244 || profile_idc == 44 || profile_idc == 83 ||
                        profile_idc == 86 || profile_idc == 118 || profile_idc == 128 ||
                                profile_idc == 138 || profile_idc == 139 || profile_idc == 134 || profile_idc == 135) {
            chroma_format_idc = ue(spsData);
            if (chroma_format_idc == 3) {
                u(spsData, 1);      //separate_colour_plane_flag
            }

            int bit_depth_luma_minus8 = ue(spsData);        //bit_depth_luma_minus8
            int bit_depth_chroma_minus8 = ue(spsData);        //bit_depth_chroma_minus8
            u(spsData, 1);      //qpprime_y_zero_transform_bypass_flag

            if (DEBUG) {
                Log.e(TAG, "seq_parameter_set_id" + seq_parameter_set_id
                                  +",chroma_format_idc:" + chroma_format_idc
                                  +",bit_depth_luma_minus8:" + bit_depth_luma_minus8
                                  +",bit_depth_chroma_minus8:" + bit_depth_chroma_minus8);
            }
            int seq_scaling_matrix_present_flag = u(spsData, 1);
            if (seq_scaling_matrix_present_flag > 0) {
                int[] seq_scaling_list_present_flag = new int[12];
                for (int i=0; i<((chroma_format_idc != 3)?8:12); i++) {
                    seq_scaling_list_present_flag[i] = u(spsData, 1);
                    if (seq_scaling_list_present_flag[i] > 0) {
                        if (i < 6) {    //scaling_list(ScalingList4x4[i], 16, UseDefaultScalingMatrix4x4Flag[I])
                        } else {    //scaling_list(ScalingList8x8[i − 6], 64, UseDefaultScalingMatrix8x8Flag[i − 6] )
                        }
                    }
                }
            }

            ue(spsData);        //log2_max_frame_num_minus4
            int pic_order_cnt_type = ue(spsData);
            if (pic_order_cnt_type == 0) {
                ue(spsData);        //log2_max_pic_order_cnt_lsb_minus4
            } else if (pic_order_cnt_type == 1) {
                u(spsData, 1);      //delta_pic_order_always_zero_flag
                se(spsData);        //offset_for_non_ref_pic
                se(spsData);        //offset_for_top_to_bottom_field

                int num_ref_frames_in_pic_order_cnt_cycle = ue(spsData);
                int[] offset_for_ref_frame = new int[num_ref_frames_in_pic_order_cnt_cycle];
                for (int i = 0; i<num_ref_frames_in_pic_order_cnt_cycle; i++) {
                    offset_for_ref_frame[i] = se(spsData);
                }
            }

            ue(spsData);      //max_num_ref_frames
            u(spsData, 1);      //gaps_in_frame_num_value_allowed_flag

            int pic_width_in_mbs_minus1 = ue(spsData);     //第36位开始
            int pic_height_in_map_units_minus1 = ue(spsData);      //47
            int frame_mbs_only_flag = u(spsData, 1);

            if(DEBUG) {
                Log.e(TAG, "pic_width_in_mbs_minus1:" + pic_width_in_mbs_minus1 + ", pic_height_in_map_units_minus1:" + pic_height_in_map_units_minus1);
            }

            width = (pic_width_in_mbs_minus1 + 1) * 16;
            height = (2 - frame_mbs_only_flag) * (pic_height_in_map_units_minus1 + 1) * 16;

            Log.e(TAG, "on first paser: width:" + width + ", height:" + height);

            if (frame_mbs_only_flag > 0) {
                u(spsData, 1);      //mb_adaptive_frame_field_flag
            }

            u(spsData, 1);     //direct_8x8_inference_flag
            int frame_cropping_flag = u(spsData, 1);
            if (frame_cropping_flag > 0) {
                int frame_crop_left_offset = ue(spsData);
                int frame_crop_right_offset = ue(spsData);
                int frame_crop_top_offset = ue(spsData);
                int frame_crop_bottom_offset= ue(spsData);

                //See 6.2 Source, decoded, and output picture formats
                int crop_unit_x = 1;
                int crop_unit_y = 2 - frame_mbs_only_flag;      //monochrome or 4:4:4
                if (chroma_format_idc == 1) {   //4:2:0
                    crop_unit_x = 2;
                    crop_unit_y = 2 * (2 - frame_mbs_only_flag);
                } else if (chroma_format_idc == 2) {    //4:2:2
                    crop_unit_x = 2;
                    crop_unit_y = 2 - frame_mbs_only_flag;
                }

                width -= crop_unit_x * (frame_crop_left_offset + frame_crop_right_offset);
                height -= crop_unit_y * (frame_crop_top_offset + frame_crop_bottom_offset);
            }

            int vui_parameters_present_flag = u(spsData, 1);
            if (vui_parameters_present_flag > 0) {
                //vui_para_parse(&bs, info);
            }
        }

        Log.e(TAG, "paser sps width:" + width + ", height:" + height);
    }

}
