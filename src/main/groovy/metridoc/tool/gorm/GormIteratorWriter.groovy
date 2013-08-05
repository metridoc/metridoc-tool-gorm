package metridoc.tool.gorm

import metridoc.iterators.Record
import metridoc.iterators.RecordIterator
import metridoc.writers.DefaultIteratorWriter
import metridoc.writers.WriteResponse

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class GormIteratorWriter extends DefaultIteratorWriter {

    Class gormClass
    GormTool gormTool

    @Override
    WriteResponse write(RecordIterator recordIterator) {
        if (gormTool) {
            if (!gormTool.ran.get()) {
                gormTool.enableGormFor(gormClass)
            }
        }
        assert gormClass: "gormClass must not be null"

        def response

        //try catch forces a failure in transaction to trigger rollback
        try {
            gormClass.withTransaction {
                response = super.write(recordIterator)
                if (response.errorTotal) {
                    throw response.fatalErrors[0]
                }
            }
        }
        catch (Throwable throwable) {
            response = new WriteResponse()
            response.addError(throwable)
        }

        return response
    }

    @Override
    boolean doWrite(int lineNumber, Record record) {
        def instance = new MetridocRecordGorm(entityInstance: gormClass.newInstance())
        if (instance.acceptRecord(record)) {
            instance.populate(record)

            if (instance.shouldSave()) {
                instance.save()
                return true
            }
        }

        return false
    }
}