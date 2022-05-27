// #include<jni.h>
// #include <cstdio>
// #include <vector>
// #include "com_retina_demo_RetinaServiceImpl.h"
// #include "rocksdb/slice.h"
// #include "RetinaService.h"

// using Slice = rocksdb::Slice;

// static RetinaService service;

// /*
//  * Class:     com_retina_demo_RetinaServiceImpl
//  * Method:    updateRecord
//  * Signature: (Lcom/retina/demo/UpdateRecordOptions;)Z
//  */
// JNIEXPORT jboolean JNICALL
// Java_com_retina_demo_RetinaServiceImpl_updateRecord
//   (JNIEnv * env, jobject retinaServiceImpl, jobject option) {
//     // access 	String schemaName;
//     //	String tableName;
//     //	int pk_id;
//     //	Value[] values;
//     //	long timestamp;
//     jclass option_class =
//     env->FindClass("Lcom/retina/demo/UpdateRecordOptions;"); jclass
//     value_class = env->FindClass("Lcom/retina/demo/Value");

//     jfieldID schema_name_fid = env->GetFieldID(option_class, "schemaName",
//     "Ljava/lang/String;"); jfieldID table_name_fid =
//     env->GetFieldID(option_class, "tableName", "Ljava/lang/String;");
//     jfieldID pk_id_fid = env->GetFieldID(option_class, "pk_id", "I");
//     jfieldID values_fid = env->GetFieldID(option_class, "values",
//     "[Lcom/retina/demo/Value"); jfieldID value_value_fid =
//     env->GetFieldID(value_class, "value", "[B"); jfieldID value_size_fid =
//     env->GetFieldID(value_class, "size", "I"); jfieldID value_type_fid =
//     env->GetFieldID(value_class, "type", "I"); jfieldID timestamp_fid =
//     env->GetFieldID(option_class, "timestamp", "J");

//     jboolean isCopy;
//     jstring schema_name_object = env->GetObjectField(option,
//     schema_name_fid); const char* schema_name = GetStringUTFChars(env,
//     schema_name_object, &isCopy); jstring table_name_object =
//     env->GetObjectField(option, table_name_fid); const char* table_name =
//     GetStringUTFChars(env, table_name_object, &isCopy);

//     jint pk_id = env->GetIntField(option, pk_id_fid);
//     jobject values_object =  env->GetObjectField(option, values_fid);
//     jlong timestamp = env->GetLongField(option, timestamp_fid);

//     jobjectArray* values_array =
//     reinterpret_cast<jobjectArray*>(&values_object); jint values_len =
//     env->GetArrayLength(values_array);

//     std::vector<Slice> record;
//     for(int i = 0; i < values_len; i++) {
//         jobject value = (jobject) env->GetObjectArrayElement(values_array,
//         i); jobject value_value = env->GetObjectField(value,
//         value_value_fid); jbyteArray* value_value_array =
//         reinterpret_cast<jbyteArray*>(&values_object); jint value_size =
//         env->GetArrayLength(value_value_array);
//        // jint value_size = env->GetIntField(value, value_size_fid);
//         jint value_type = env->GetIntField(value, value_type_fid);
//         record.push_back(Slice((char*)value_value_array, value_size));
//     }

//     service.update(schema_name, table_name, (int)pk_id, record,
//     (long)timstamp);

// }

// /*
//  * Class:     com_retina_demo_RetinaServiceImpl
//  * Method:    insertRecord
//  * Signature: (Lcom/retina/demo/InsertRecordOptions;)Z
//  */
// JNIEXPORT jboolean JNICALL
// Java_com_retina_demo_RetinaServiceImpl_insertRecord
//   (JNIEnv * env, jobject retinaServiceImpl, jobject option) {

// }

// /*
//  * Class:     com_retina_demo_RetinaServiceImpl
//  * Method:    deleteRecord
//  * Signature: (Lcom/retina/demo/DeleteRecordOptions;)Z
//  */
// JNIEXPORT jboolean JNICALL
// Java_com_retina_demo_RetinaServiceImpl_deleteRecord
//   (JNIEnv * env, jobject retinaServiceImpl, jobject option) {

// }
