##
# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
# 
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
# 
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
# 
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
##

# File auto-generated against equivalent DynamicSerialize Java class

import abc
import os
from dynamicserialize.dstypes.com.raytheon.uf.common.auth.user import User

class AbstractLocalizationStreamRequest(object):
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def __init__(self):
        self.context = None
        self.fileName = None
        self.myContextName = None
        self.user = User()

    def getContext(self):
        return self.context

    def setContext(self, context):
        self.context = context

    def getFileName(self):
        return self.fileName

    def setFileName(self, fileName):
        if fileName[0] == os.sep:
            fileName = fileName[1:] 
        self.fileName = fileName
        
    def getMyContextName(self):
        return self.myContextName

    def setMyContextName(self, contextName):
        self.myContextName = str(contextName)

    def getUser(self):
        return self.user

    def setUser(self, user):
        self.user = user
