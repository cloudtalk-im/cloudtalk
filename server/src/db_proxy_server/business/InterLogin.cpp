
#include "InterLogin.h"
#include "../DBPool.h"
#include "EncDec.h"

bool CInterLoginStrategy::doLogin(const std::string &strName, const std::string &strToken, IM::BaseDefine::UserInfo& user)
{
    bool bRet = false;
    try {
        CDBManager *pDBManger = CDBManager::getInstance();
        CDBConn *pDBConn = pDBManger->GetDBConn("teamtalk_slave");
        if (pDBConn) {
            string strSql = "select * from on_IMUser where id='" + strName + "' and status=0";
            CResultSet *pResultSet = pDBConn->ExecuteQuery(strSql.c_str());
            if (pResultSet->Next() && pResultSet->GetInt("id") != NULL && pResultSet->GetInt("id") > 0) {
                string strResult, strSalt;
                uint32_t nId, nGender, nDeptId, nStatus;
                string strNick, strAvatar, strEmail, strRealName, strTel, strDomain, strSignInfo;
                do {
                        nId = pResultSet->GetInt("id");
                        strResult = pResultSet->GetString("api_token");
                        strSalt = pResultSet->GetString("salt");
                        strNick = pResultSet->GetString("nickname");
                        nGender = pResultSet->GetInt("sex");
                        strRealName = pResultSet->GetString("realname");
                        strDomain = pResultSet->GetString("domain");
                        strTel = pResultSet->GetString("phone");
                        strEmail = pResultSet->GetString("email");
                        strAvatar = pResultSet->GetString("avatar");
                        nDeptId = pResultSet->GetInt("departId");
                        nStatus = pResultSet->GetInt("status");
                        strSignInfo = pResultSet->GetString("sign_info");

                } while (pResultSet->Next());
                if (strToken == strResult) //验证token是否有效
                {
                    user.set_user_id(nId);
                    user.set_user_nick_name(strNick);
                    user.set_user_gender(nGender);
                    user.set_user_real_name(strRealName);
                    user.set_user_domain(strDomain);
                    user.set_user_tel(strTel);
                    user.set_email(strEmail);
                    user.set_avatar_url(strAvatar);
                    user.set_department_id(nDeptId);
                    user.set_status(nStatus);
                    user.set_sign_info(strSignInfo);
                    bRet = true;
                }
                delete pResultSet;
            }
            pDBManger->RelDBConn(pDBConn);
        }
    }
    catch(exception& e2) {
    log("error-------读取用户信息错误! bool CInterLoginStrategy::doLogin 2(:%s"); }
    catch (...) {}

    return bRet;
}
