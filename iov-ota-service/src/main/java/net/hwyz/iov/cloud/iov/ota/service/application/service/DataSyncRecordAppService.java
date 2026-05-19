package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DataSource;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DataType;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.DataSyncRecordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.DataSyncRecordPo;
import org.springframework.stereotype.Service;

/**
 * 数据同步记录应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncRecordAppService {

    private final DataSyncRecordMapper dataSyncRecordMapper;

    /**
     * 新增数据同步记录
     *
     * @param dataSource 数据来源
     * @param dataType   数据类型
     * @param code       代码
     * @param data       数据
     * @return 结果
     */
    public DataSyncRecordPo createDataSyncRecord(DataSource dataSource, DataType dataType, String code, String data) {
        DataSyncRecordPo dataSyncRecord = DataSyncRecordPo.builder()
                .source(dataSource.value)
                .type(dataType.value)
                .code(code)
                .data(data)
                .state(0)
                .build();
        dataSyncRecordMapper.insertPo(dataSyncRecord);
        return dataSyncRecord;
    }

    /**
     * 标记数据同步成功
     *
     * @param dataSyncRecord 数据同步记录
     */
    public void markRecordSuccess(DataSyncRecordPo dataSyncRecord) {
        dataSyncRecord.setState(1);
        dataSyncRecordMapper.updatePo(dataSyncRecord);
    }

    /**
     * 标记数据同步失败
     *
     * @param dataSyncRecord 数据同步记录
     */
    public void markRecordFail(DataSyncRecordPo dataSyncRecord, String errorMsg) {
        dataSyncRecord.setState(-1);
        dataSyncRecord.setDescription(errorMsg);
        dataSyncRecordMapper.updatePo(dataSyncRecord);
    }

}
