#include "DepartModel.h"
#include "../DBPool.h"

CDepartModel* CDepartModel::m_pInstance = NULL;

CDepartModel* CDepartModel::getInstance()
{
    if(NULL == m_pInstance)
    {
        m_pInstance = new CDepartModel();
    }
    return m_pInstance;
}

/**
 *
 * @param nUserId
 * @param nLastTime
 * @param lsDepts
 */
void CDepartModel::getDepts(uint32_t& nUserId, uint32_t& nLastTime, list<IM::BaseDefine::DepartInfo>& lsDepts)
{
    CDBManager* pDBManager = CDBManager::getInstance();
    CDBConn* pDBConn = pDBManager->GetDBConn("teamtalk_slave");
    if (pDBConn)
    {

        string strSql = "select * from on_IMDepart where (uid=0 || uid="+int2string(nUserId)+") and updated > " + int2string(nLastTime);
        CResultSet* pResultSet = pDBConn->ExecuteQuery(strSql.c_str());
        if(pResultSet)
        {
            while (pResultSet->Next()) {
                IM::BaseDefine::DepartInfo cDept;
                uint32_t nId = pResultSet->GetInt("id");
                uint32_t nParentId = pResultSet->GetInt("parentId");
                string strDeptName = pResultSet->GetString("departName");
                uint32_t nStatus = pResultSet->GetInt("status");
                uint32_t nPriority = pResultSet->GetInt("priority");
                if(IM::BaseDefine::DepartmentStatusType_IsValid(nStatus))
                {
                    cDept.set_dept_id(nId);
                    cDept.set_parent_dept_id(nParentId);
                    cDept.set_dept_name(strDeptName);
                    cDept.set_dept_status(IM::BaseDefine::DepartmentStatusType(nStatus));
                    cDept.set_priority(nPriority);
                    lsDepts.push_back(cDept);
                }
            }
            delete  pResultSet;
        }
        pDBManager->RelDBConn(pDBConn);
    }
    else
    {
        log("no db connection for teamtalk_slave");
    }
}

void CDepartModel::getDept(uint32_t nDeptId, IM::BaseDefine::DepartInfo& cDept)
{
    CDBManager* pDBManager = CDBManager::getInstance();
    CDBConn* pDBConn = pDBManager->GetDBConn("teamtalk_slave");
    if (pDBConn)
    {
        string strSql = "select * from on_IMDepart where id = " + int2string(nDeptId);
        CResultSet* pResultSet = pDBConn->ExecuteQuery(strSql.c_str());
        if(pResultSet)
        {
            while (pResultSet->Next()) {
                uint32_t nId = pResultSet->GetInt("id");
                uint32_t nParentId = pResultSet->GetInt("parentId");
                string strDeptName = pResultSet->GetString("departName");
                uint32_t nStatus = pResultSet->GetInt("status");
                uint32_t nPriority = pResultSet->GetInt("priority");
                if(IM::BaseDefine::DepartmentStatusType_IsValid(nStatus))
                {
                    cDept.set_dept_id(nId);
                    cDept.set_parent_dept_id(nParentId);
                    cDept.set_dept_name(strDeptName);
                    cDept.set_dept_status(IM::BaseDefine::DepartmentStatusType(nStatus));
                    cDept.set_priority(nPriority);
                }
            }
            delete  pResultSet;
        }
        pDBManager->RelDBConn(pDBConn);
    }
    else
    {
        log("no db connection for teamtalk_slave");
    }
}